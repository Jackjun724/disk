package com.baidu.disk.algorithm;


import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * @author JackJun
 * @date 2020/11/5 5:46 下午
 */


public class Sign {

    public static String getLogId(String ip) {
        int v1 = (int) (Math.random() * 999999);
        String val = System.currentTimeMillis() + "," + ip + "," + v1;
        byte[] valByte = val.getBytes();
        return encode(valByte);
    }

    public static String getRand(String uid, String time, String devUid, String bduss, String sk) {
        String bdussSha1 = sha1(bduss);
        String keys = new String(encrypt(decode(sk), uid));
        System.out.println("bdussSha1: " + bdussSha1);
        System.out.println("uid: " + uid);
        System.out.println("sk: " + keys);
        System.out.println("time: " + time);
        System.out.println("devUid: " + devUid);
        return sha1(bdussSha1 + uid + keys + time + devUid);
    }

    public static String getBdstoken(String bduss) {
        return shaHex(md5(bduss.getBytes(StandardCharsets.UTF_8)));
    }

    @SneakyThrows
    public static byte[] md5(byte[] bduss) {
        MessageDigest digest;
        byte[] res = null;
        if (bduss != null && bduss.length != 0) {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(bduss, 0, bduss.length);
            res = digest.digest();
        }
        return res;
    }

    @SneakyThrows
    public static String sha1(String msg) {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(msg.getBytes());
        return shaHex(digest.digest());
    }

    public static String shaHex(byte[] digest) {
        StringBuilder str = new StringBuilder();
        for (byte b : digest) {
            String res = Integer.toHexString(b & 255);
            if (res.length() == 1) {
                str.append("0");
            }
            str.append(res);
        }
        return str.toString();
    }

    public static byte[] encrypt(byte[] content, String key) {
        // decompiled BaiduYun Disk v11 for android
        int bit = 256;
        int[] v1 = new int[bit];
        byte[] v2 = new byte[bit];

        for (int i = 0; i < bit; i++)
            v1[i] = i;


        for (int i = 0; i < bit; i++)
            v2[i] = (byte) key.charAt((i % key.length()));

        int j = 0;

        for (int i = 0; i < 256; i++) {
            j = (j + v1[i] + v2[i]) % bit;
            int temp = v1[i];
            v1[i] = v1[j];
            v1[j] = temp;
        }


        int i = 0;
        j = 0;
        byte[] iOutput = new byte[content.length];
        for (short x = 0; x < content.length; x++) {
            i = (i + 1) % bit;
            j = (j + v1[i]) % bit;
            int temp = v1[i];
            v1[i] = v1[j];
            v1[j] = temp;
            char iCY = (char) v1[(v1[i] + (v1[j] % bit)) % bit];
            iOutput[x] = (byte) (content[x] ^ iCY);
        }

        return iOutput;
    }

    public static String encode(byte[] val) {
        return Base64.getEncoder().encodeToString(val);
    }

    public static byte[] decode(String val) {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(val);
    }

}
