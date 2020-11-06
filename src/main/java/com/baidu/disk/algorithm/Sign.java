package com.baidu.disk.algorithm;


import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.script.*;
import java.io.InputStreamReader;

/**
 * @author JackJun
 * @date 2020/11/5 5:46 下午
 */


public class Sign {

    private final static ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");

    @SneakyThrows
    public static String getLogId(String id) {
        // 获取JS执行引擎
        ResourceLoader loader = new DefaultResourceLoader();

        se.eval(new InputStreamReader(loader.getResource("classpath:static/log.js").getInputStream()));
        // 是否可调用
        if (se instanceof Invocable) {
            Invocable in = (Invocable) se;
            return (String) in.invokeFunction("getLogID", id);
        }
        return "";
    }

    public static String getDevUid(byte[] bdussBytes) {
        return "0|" + DigestUtils.md5Hex(bdussBytes).toUpperCase();
    }

    public static String getRand(long uid, long time, String devUid, byte[] bdussBytes) {
        String bdussSha1 = DigestUtils.sha1Hex(bdussBytes);
        String keys = "ebrcUYiuxaZv2XGu7KIYKxUrqfnOfpDF";
        return DigestUtils.sha1Hex((bdussSha1 + uid + keys + time + devUid).getBytes());
    }
}
