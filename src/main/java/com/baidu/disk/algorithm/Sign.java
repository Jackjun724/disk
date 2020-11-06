package com.baidu.disk.algorithm;


import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author JackJun
 * @date 2020/11/5 5:46 下午
 */


public class Sign {

    public static String getDevUid(byte[] bdussBytes) {
        return "0|" + DigestUtils.md5Hex(bdussBytes).toUpperCase();
    }

    public static String getRand(long uid, long time, String devUid, byte[] bdussBytes) {
        String bdussSha1 = DigestUtils.sha1Hex(bdussBytes);
        String keys = "ebrcUYiuxaZv2XGu7KIYKxUrqfnOfpDF";
        return DigestUtils.sha1Hex((bdussSha1 + uid + keys + time + devUid).getBytes());
    }
}
