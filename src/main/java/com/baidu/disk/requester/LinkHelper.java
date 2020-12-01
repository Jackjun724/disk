package com.baidu.disk.requester;

import com.baidu.disk.algorithm.Sign;
import com.baidu.disk.algorithm.SoSign;
import com.baidu.disk.config.BaiduYunProperties;
import com.baidu.disk.web.exception.ExpireException;
import com.baidu.disk.web.exception.ServiceException;
import com.baidu.disk.web.vo.DownloadUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLDecoder;
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheEntity {
        private String shareId;
        private String uk;
        private String pwd;
        private String fsId;
        private String dir;
        private String codeStr;
        private Boolean root;
    }

    @Data
    @Builder
    public static class Captcha {
        private String codeStr;
        private String captchaUrl;
    }

    private final SoSign soSign;

    public static final String UA = "netdisk;8.8.0;MuMu;android-android;6.0.1;JSbridge3.0.0";

    private final BaiduYunProperties baiduYunProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(false).build();

    private final String time = "1606712249691";
    private final String rand = "b37a9bfc4cfa5f837147ca19c82449c5343247cf";

    public Captcha getCaptcha() throws IOException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://pan.baidu.com/api/getcaptcha?devuid=" + baiduYunProperties.getDevUid())).newBuilder()
                .addQueryParameter("clienttype", "1")
                .addQueryParameter("channel", "android_6.0.1_MuMu_bd-netdisk_1018849x")
                .addQueryParameter("version", "8.8.0")
                .addQueryParameter("logid", Sign.getLogId(baiduYunProperties.getStoken()))
                .addQueryParameter("vip", "2")
                .addQueryParameter("time", time)
                .addQueryParameter("cuid", baiduYunProperties.getCuid())
                .addQueryParameter("network_type", "wifi")
                .addQueryParameter("apn_id", "1_0")
                .addQueryParameter("freeisp", "0")
                .addQueryParameter("queryfree", "0")
                .addQueryParameter("rand", rand)
                .build();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "appid=250528&prod=shareverify&bdstoken=" + Sign.getBdstoken(baiduYunProperties.getBduss()));
        Map resp = post(url, body);
        String codeStr = resp.get("vcode_str").toString();
        String codeUrl = resp.get("vcode_img").toString();
        return Captcha.builder().codeStr(codeStr).captchaUrl(codeUrl).build();
    }

    @SuppressWarnings("rawtypes")
    private Map post(HttpUrl url, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(url.url().toString())
                .method("POST", body)
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .addHeader("User-Agent", UA)
                .build();
        return objectMapper.readValue(Objects.requireNonNull(client.newCall(request).execute().body()).string(), Map.class);
    }

    @SuppressWarnings("rawtypes")
    public String verify(String shareId, String uk, String pwd, String codeStr, String code) throws IOException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://pan.baidu.com/share/verify?shareid=" + shareId)).newBuilder()
                .addQueryParameter("uk", uk)
                .addQueryParameter("devuid", baiduYunProperties.getDevUid())
                .addQueryParameter("clienttype", "12")
                .addQueryParameter("channel", "android_6.0.1_MuMu_bd-netdisk_1018849x")
                .addQueryParameter("version", "8.8.0")
                .addQueryParameter("logid", Sign.getLogId(baiduYunProperties.getStoken()))
                .addQueryParameter("vip", "2")
                .addQueryParameter("time", time)
                .addQueryParameter("cuid", baiduYunProperties.getCuid())
                .addQueryParameter("network_type", "wifi")
                .addQueryParameter("apn_id", "1_0")
                .addQueryParameter("freeisp", "0")
                .addQueryParameter("queryfree", "0")
                .addQueryParameter("rand", rand)
                .build();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "pwd=" + pwd + "&vcode=" + (code == null ? "" : code) + "&vcode_str=" + (codeStr == null ? "null" : codeStr) + "&bdstoken=" + Sign.getBdstoken(baiduYunProperties.getBduss()));

        Map resp = post(url, body);
        int errno = Integer.parseInt(resp.get("errno").toString());

        if (errno == -62) {
            return null;
        } else if (errno == 0) {
            return URLDecoder.decode(resp.get("randsk").toString(), "UTF-8");
        } else {
            throw new ExpireException();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DownloadUrl getDLink(String shareId, String uk, String pwd, String path, boolean root, String codeStr, String code) throws IOException {
        String randsk = verify(shareId, uk, pwd, codeStr, code);

        if (randsk == null) {
            Captcha captcha = getCaptcha();
            return DownloadUrl.builder()
                    .captchaUrl(captcha.getCaptchaUrl())
                    .codeStr(captcha.getCodeStr())
                    .build();
        }

        String timestamp = null, sign = null;
        try {
            timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String source = shareId + "_" + uk + "_" + baiduYunProperties.getDevUid() + "_" + timestamp;
            String key = soSign.generate();
            sign = Sign.customEncode(Sign.hmacSHA1Encrypt(key.getBytes(), (source).getBytes()), "", false);
            log.info("sign {}, time: {}, source: {}, key: {}", sign, timestamp, source, key);
        } catch (Exception e) {
            //ignore
        }

        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert servletRequestAttributes != null;

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://pan.baidu.com/share/list?shareid=" + shareId)).newBuilder()
                .addQueryParameter("uk", uk)
                .addQueryParameter("root", "1")
                .addQueryParameter("sekey", randsk)
                .addQueryParameter("sign", sign)
                .addQueryParameter("timestamp", timestamp)
                .addQueryParameter("bdstoken", Sign.getBdstoken(baiduYunProperties.getBduss()))
                .addQueryParameter("devuid", baiduYunProperties.getDevUid())
                .addQueryParameter("clienttype", "1")
                .addQueryParameter("channel", "android_6.0.1_MuMu_bd-netdisk_1018849x")
                .addQueryParameter("version", "8.8.0")
                .addQueryParameter("logid", Sign.getLogId(baiduYunProperties.getStoken()))
                .addQueryParameter("vip", "2")
                .addQueryParameter("time", time)
                .addQueryParameter("cuid", baiduYunProperties.getCuid())
                .addQueryParameter("network_type", "wifi")
                .addQueryParameter("apn_id", "1_0")
                .addQueryParameter("freeisp", "0")
                .addQueryParameter("queryfree", "0")
                .addQueryParameter("rand", rand)
                .build();


        String realUrl = soSign.handlerUrl(url.url().toString(), getSK());

        Request request = new Request.Builder()
                .url(realUrl)
                .method("GET", null)
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .addHeader("User-Agent", UA)
                .build();



        Map resp = objectMapper.readValue(Objects.requireNonNull(client.newCall(request).execute().body()).string(), Map.class);

        // 修正path
        path = path.substring(1);
        path = path.substring(path.indexOf("/"));
        String title = resp.get("title").toString();
        title = title.substring(0, title.lastIndexOf("/"));
        path =  title + path;

        if (!root) {
            String dir = path.substring(0, path.lastIndexOf("/"));

            log.info("Dir {},", path);

            url = Objects.requireNonNull(HttpUrl.parse("https://pan.baidu.com/share/list?shareid=" + shareId)).newBuilder()
                    .addQueryParameter("uk", uk)
                    .addQueryParameter("dir", dir)
                    .addQueryParameter("sekey", randsk)
                    .addQueryParameter("sign", sign)
                    .addQueryParameter("timestamp", timestamp)
                    .addQueryParameter("bdstoken", Sign.getBdstoken(baiduYunProperties.getBduss()))
                    .addQueryParameter("devuid", baiduYunProperties.getDevUid())
                    .addQueryParameter("clienttype", "1")
                    .addQueryParameter("channel", "android_6.0.1_MuMu_bd-netdisk_1018849x")
                    .addQueryParameter("version", "8.8.0")
                    .addQueryParameter("logid", Sign.getLogId(baiduYunProperties.getStoken()))
                    .addQueryParameter("vip", "2")
                    .addQueryParameter("time", time)
                    .addQueryParameter("cuid", baiduYunProperties.getCuid())
                    .addQueryParameter("network_type", "wifi")
                    .addQueryParameter("apn_id", "1_0")
                    .addQueryParameter("freeisp", "0")
                    .addQueryParameter("queryfree", "0")
                    .addQueryParameter("rand", rand)
                    .build();

            request = new Request.Builder()
                    .url(url.url().toString())
                    .method("GET", null)
                    .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                    .addHeader("User-Agent", UA)
                    .build();
            resp = objectMapper.readValue(Objects.requireNonNull(client.newCall(request).execute().body()).string(), Map.class);
        }
        List<Map<String, Object>> dlinks = (List<Map<String, Object>>) resp.get("list");
        log.info("Url {},", realUrl);
        String realDlink = null;
        log.info("Resp {}", resp);
        for (Map<String, Object> dlink : dlinks) {
            if (path.equalsIgnoreCase(dlink.get("path").toString())) {
                realDlink = dlink.get("dlink").toString();
            }
        }

        if (realDlink == null) {
            log.error("不存在的文件");
            throw new ServiceException("不存在的文件");
        }

        request = new Request.Builder()
                .url(realDlink)
                .method("GET", null)
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .addHeader("User-Agent", "LogStatistic")
                .build();

        Response response = client.newCall(request).execute();

        String link = response.header("Location");

        return DownloadUrl.builder().url(link).ua("LogStatistic").build();
//        if (Integer.parseInt(String.valueOf(resp.get("errno"))) == 0) {
//            String path;
//            String params = realDlink.substring(realDlink.indexOf("?") + 1);
//            path = realDlink.substring(realDlink.indexOf("file/") + 5, realDlink.indexOf('?'));
//            return getLink(path, params);
//        }
//        throw new ExpireException();
    }

    /**
     * @param path   file md5
     * @param params params
     * @return Map
     * @throws IOException request Exception
     */
    @SuppressWarnings("unchecked")
    public DownloadUrl getLink(String path, String params) throws IOException {
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
                        .addQueryParameter("time", time)
                        .addQueryParameter("cuid", baiduYunProperties.getCuid())
                        .addQueryParameter("network_type", "wifi")
                        .addQueryParameter("apn_id", "1_0")
                        .addQueryParameter("freeisp", "0")
                        .addQueryParameter("queryfree", "0")
                        .addQueryParameter("rand", rand)
                        .build())
                .build();
        Request request = new Request.Builder().url(soSign.handlerUrl(urlGenerate.url().toString(), getSK()))
                .method("GET", null)
                .addHeader(HttpHeaders.HOST, "d.pcs.baidu.com")
                .addHeader(HttpHeaders.USER_AGENT, UA)
                .addHeader(HttpHeaders.COOKIE, "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .build();
        log.info(request.url().toString());
        String response = Objects.requireNonNull(client.newCall(request).execute().body()).string();
        log.info("Response ==> {}", response);
        return DownloadUrl.builder().ua(UA).url(((List<Map<String, Object>>) objectMapper.readValue(response, Map.class).get("urls")).get(0).get("url").toString()).build();
    }

    public String getSK() throws IOException {
        Request request = new Request.Builder()
                .url("https://pan.baidu.com/api/report/user?action=ANDROID_ACTIVE_FRONTDESK&clienttype=1&version=8.8.0&channel=android_8.0.1_XiaoMi_bd-netdisk_1001528p")
                .method("GET", null)
                .addHeader("Cookie", "BDUSS=" + baiduYunProperties.getBduss() + "; STOKEN=" + baiduYunProperties.getStoken() + ";")
                .addHeader("User-Agent", UA)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Host", "pan.baidu.com")
                .build();
        String response = Objects.requireNonNull(client.newCall(request).execute().body()).string();
        log.debug("SK Response ==> {}", response);
        return (String) objectMapper.readValue(response, Map.class).get("uinfo");
    }

}
