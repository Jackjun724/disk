package com.baidu.disk.requester;

import com.baidu.disk.algorithm.Sign;
import com.baidu.disk.algorithm.SoSign;
import com.baidu.disk.config.BaiduYunProperties;
import com.baidu.disk.web.exception.ExpireException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author JackJun
 * @date 2020/11/6 1:20 下午
 */
@Slf4j
@Component
@AllArgsConstructor
public class LinkHelper {

    private final SoSign soSign;

    public static final String UA = "netdisk;P2SP;2.2.41.20;netdisk;8.8.0;MuMu;android-android;6.0.1;JSbridge4.0.0";

    private final BaiduYunProperties baiduYunProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();


    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<?, ?> getDLink(String fsId, String timestamp, String sign, String randsk, String shareId, String uk) throws IOException {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert servletRequestAttributes != null;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
        String encrypt = URLEncoder.encode("{\"sekey\":\"" + randsk + "\"}", "UTF-8");
        RequestBody body = RequestBody.create(mediaType, "encrypt=0&extra=" + encrypt + "&product=share&uk=" + uk + "&primaryid=" + shareId + "&fid_list=[" + fsId + "]");
        Request request = new Request.Builder()
                .url("https://pan.baidu.com/api/sharedownload?sign=" + sign + "&timestamp=" + timestamp + "&channel=chunlei&web=1&app_id=250528&clienttype=5&logid=" + Sign.getLogId(baiduYunProperties.getStoken()))
                .method("POST", body)
                .addHeader(HttpHeaders.CONNECTION, "keep-alive")
                .addHeader(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01")
                .addHeader(HttpHeaders.X_REQUESTED_WITH, "XMLHttpRequest")
                .addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36")
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader(HttpHeaders.ORIGIN, "https://pan.baidu.com")
                .addHeader(HttpHeaders.REFERER, "https://pan.baidu.com/")
                .addHeader(HttpHeaders.COOKIE, "BDCLND=" + randsk + "; BDUSS=" + baiduYunProperties.getBduss() + ";")
                .build();
        Map resp = objectMapper.readValue(Objects.requireNonNull(client.newCall(request).execute().body()).string(), Map.class);
        if (Integer.parseInt(String.valueOf(resp.get("errno"))) == 0) {
            String dLink = (String) ((List<Map<String, Object>>) resp.get("list")).get(0).get("dlink");
            String path;
            String params = dLink.substring(dLink.indexOf("?") + 1);
            path = dLink.substring(dLink.indexOf("file/") + 5, dLink.indexOf('?'));
            return getLink(path, params);
        }
        throw new ExpireException();
    }

    /**
     * @param path   file md5
     * @param params params
     * @return Map
     * @throws IOException request Exception
     */
    public Map<?, ?> getLink(String path, String params) throws IOException {
        long time = System.currentTimeMillis();
        Request urlGenerate = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse("https://d.pcs.baidu.com/rest/2.0/pcs/file?method=locatedownload&path=" + path + "&" + params)).newBuilder()
                        .addQueryParameter("ver", "2.0")
                        .addQueryParameter("dtype", "0")
                        .addQueryParameter("esl", "1")
                        .addQueryParameter("ehps", "0")
                        .addQueryParameter("app_id", "250528")
                        .addQueryParameter("check_blue", "1")
                        .addQueryParameter("bdstoken", Sign.getBdstoken(baiduYunProperties.getBduss()))
                        .addQueryParameter("devuid", baiduYunProperties.getDevUid())
                        .addQueryParameter("clienttype", "1")
                        .addQueryParameter("channel", "android_6.0.1_MuMu_bd-netdisk_1018849x")
                        .addQueryParameter("version", "8.8.0")
                        .addQueryParameter("logid", Sign.getLogId(baiduYunProperties.getStoken()))
                        .addQueryParameter("vip", "2")
                        .addQueryParameter("time", String.valueOf(time))
                        .addQueryParameter("cuid", baiduYunProperties.getCuid())
                        .addQueryParameter("network_type", "wifi")
                        .addQueryParameter("apn_id", "1_0")
                        .addQueryParameter("freeisp", "0")
                        .addQueryParameter("queryfree", "0")
                        .build())
                .build();

        Request request = new Request.Builder().url(soSign.handlerUrl(urlGenerate.url().toString(), getSK()))
                .method("GET", null)
                .addHeader(HttpHeaders.HOST, "d.pcs.baidu.com")
                .addHeader(HttpHeaders.USER_AGENT, UA)
                .addHeader(HttpHeaders.COOKIE, "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .build();

        log.debug(request.url().toString());
        String response = Objects.requireNonNull(client.newCall(request).execute().body()).string();
        log.debug("Response ==> {}", response);
        return objectMapper.readValue(response, Map.class);
    }

    public String getSK() throws IOException {
        Request request = new Request.Builder()
                .url("https://pan.baidu.com/api/report/user?action=ANDROID_ACTIVE_FRONTDESK&clienttype=1&version=8.8.0&channel=android_8.0.1_XiaoMi_bd-netdisk_1001528p")
                .method("GET", null)
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .addHeader("User-Agent", "netdisk;11.2.4;XiaoMi;android-android;8.0.1;JSbridge4.4.0")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Host", "pan.baidu.com")
                .build();
        String response = Objects.requireNonNull(client.newCall(request).execute().body()).string();
        log.debug("Response ==> {}", response);
        return (String) objectMapper.readValue(response, Map.class).get("uinfo");
    }

}
