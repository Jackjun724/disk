package com.baidu.disk.requester;

import com.baidu.disk.algorithm.Sign;
import com.baidu.disk.config.BaiduYunProperties;
import com.baidu.disk.web.exception.ExpireException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author JackJun
 * @date 2020/11/6 1:20 下午
 */
@Component
@AllArgsConstructor
public class LinkHelper {

    private final BaiduYunProperties baiduYunProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<?, ?> getDLink(String fsId, String timestamp, String sign, String randsk, String shareId, String uk) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
        String encrypt = URLEncoder.encode("{\"sekey\":\"lKLU9KSNbpwBgx/J278c+8EryrADDXfD\"}", "UTF-8");
        RequestBody body = RequestBody.create(mediaType, "encrypt=0&extra=" + encrypt + "&product=share&type=nolimit&uk=" + uk + "&primaryid=" + shareId + "&fid_list=[" + fsId + "]&path_list=&vip=2");
        Request request = new Request.Builder()
                .url("https://pan.baidu.com/api/sharedownload?sign=" + sign + "&timestamp=" + timestamp + "&channel=chunlei&web=1&app_id=250528&clienttype=5")
                .method("POST", body)
                .addHeader("Connection", "keep-alive")
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Origin", "https://pan.baidu.com")
                .addHeader("Referer", "https://pan.baidu.com/")
                .addHeader("Cookie", "BDCLND=" + randsk + "; BDUSS=" + baiduYunProperties.getBduss() + ";")
                .build();
        Map resp = objectMapper.readValue(Objects.requireNonNull(client.newCall(request).execute().body()).string(), Map.class);
        if (Integer.parseInt(String.valueOf(resp.get("errno"))) == 0) {
            String dLink = (String) ((List<Map<String, Object>>) resp.get("list")).get(0).get("dlink");
            String path, fid, dsTime, sign2, vuk;
            Map<String, String> param = parse(dLink);
            path = dLink.substring(dLink.indexOf("file/") + 5, dLink.indexOf('?'));
            fid = param.get("fid");
            dsTime = param.get("dstime");
            sign2 = URLDecoder.decode(param.get("sign"), "UTF-8");
            vuk = param.get("vuk");
            return getLink(path, fid, dsTime, sign2, vuk);
        }
        throw new ExpireException();
    }

    /**
     * @param path   file md5
     * @param fid    file id
     * @param dsTime dsTime
     * @param sign   sign
     * @param vuk    vuk
     * @return Map
     * @throws IOException request Exception
     */
    public Map<?, ?> getLink(String path, String fid, String dsTime, String sign, String vuk) throws IOException {


        String devUid = Sign.getDevUid(baiduYunProperties.getBduss().getBytes());
        long time = System.currentTimeMillis() / 1000;
        String rand = Sign.getRand(baiduYunProperties.getUid(), time, devUid, baiduYunProperties.getBduss().getBytes());

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse("https://pcs.baidu.com/rest/2.0/pcs/file")).newBuilder()
                        .addQueryParameter("app_id", "250528")
                        .addQueryParameter("method", "locatedownload")
                        .addQueryParameter("path", path)
                        .addQueryParameter("ver", "2")
                        .addQueryParameter("time", String.valueOf(time))
                        .addQueryParameter("rand", rand)
                        .addQueryParameter("devuid", devUid)
                        .addQueryParameter("fid", fid)
                        .addQueryParameter("dstime", dsTime)
                        .addQueryParameter("rt", "sh")
                        .addQueryParameter("sign", sign)
                        .addQueryParameter("expires", "1h")
                        .addQueryParameter("chkv", "1")
                        .addQueryParameter("vuk", vuk)

                        .build())
                .method("GET", null)
                .addHeader("Host", "pcs.baidu.com")
                .addHeader("User-Agent", "netdisk;2.2.51.6;netdisk;10.0.63;PC;android-android")
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=")
                .build();
        String response = Objects.requireNonNull(client.newCall(request).execute().body()).string();
        return objectMapper.readValue(response, Map.class);
    }

    public static Map<String, String> parse(String url) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (url.equals("")) {
            return null;
        }
        String[] urlParts = url.split("\\?");
        //没有参数
        if (urlParts.length == 1) {
            return null;
        }
        //有参数
        String[] params = urlParts[1].split("&");
        Map<String, String> res = new HashMap<>();
        for (String param : params) {
            String[] keyValue = param.split("=");
            res.put(keyValue[0], keyValue.length == 2 ? keyValue[1] : null);
        }
        return res;
    }

}
