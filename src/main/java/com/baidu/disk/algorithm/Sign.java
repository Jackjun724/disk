package com.baidu.disk.algorithm;


import lombok.SneakyThrows;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static String getBdstoken(String bduss) {
        return shaHex(md5(bduss.getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] hmacSHA1Encrypt(byte[] arg3, byte[] arg4) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] res;
        Mac mac;
        if (arg3 != null && arg4 != null) {
            try {
                mac = Mac.getInstance("HmacSHA1");
            } catch (NoSuchAlgorithmException v0) {
                mac = Mac.getInstance("HMAC-SHA-1");
            }
            mac.init(new SecretKeySpec(arg3, "RAW"));
            res = mac.doFinal(arg4);
        } else {
            res = null;
        }
        return res;
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
    @SuppressWarnings("unused")
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

    /**
     * RC4
     *
     * @param content content
     * @param key     key
     * @return encrypt code
     */
    @SuppressWarnings("unused")
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

    public static String customEncode(byte[] bArr, String str, boolean z) {
        if (bArr == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (z) {
                hexString = hexString.toUpperCase();
            }
            if (hexString.length() == 1) {
                sb.append("0");
            }
            sb.append(hexString).append(str);
        }
        return sb.toString();
    }

    public static String encode(byte[] val) {
        return Base64.getEncoder().encodeToString(val);
    }

    /**
     * Base64 decode
     *
     * @param val val
     * @return decode val
     */
    @SuppressWarnings("unused")
    public static byte[] decode(String val) {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(val);
    }

}
