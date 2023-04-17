package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 微信SHA1签名工具类
 *
 * @author 安炯奕
 * @date 2019/11/5 17:00
 */
@Slf4j
public class WechatSignUtil {

    /**
     * 用SHA1算法验证Token
     *
     * @param token     票据
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @return 安全签名
     */
    public static String getSha1(String token, String timestamp, String nonce) {
        try {
            String[] array = new String[]{token, timestamp, nonce};
            StringBuilder sb = new StringBuilder();
            // 字符串排序
            Arrays.sort(array);
            for (int i = 0; i < 3; i++) {
                sb.append(array[i]);
            }
            String str = sb.toString();
            // SHA1签名生成
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(str.getBytes());
            byte[] digest = md.digest();

            StringBuilder hexStr = new StringBuilder();
            String shaHex;
            for (byte b : digest) {
                shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static String signBySha1(Map<String, String> data) {
        try {
            Set<String> keySet = data.keySet();
            String[] array = keySet.toArray(new String[0]);
            StringBuilder sb = new StringBuilder();
            // 字符串排序
            Arrays.sort(array);
            for (String s : array) {
                if ("sign".equals(s)) {
                    continue;
                }
                // 参数值为空，则不参与签名
                if (data.get(s).trim().length() > 0) {
                    sb.append(s).append("=").append(data.get(s).trim()).append("&");
                }
            }
            String sortedKvStr = sb.toString();
            if (sb.length() > 0) {
                sortedKvStr = sb.substring(0, sb.length() - 1);
            }
            // SHA1签名生成
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sortedKvStr.getBytes());
            byte[] digest = md.digest();

            StringBuilder hexStr = new StringBuilder();
            String shaHex;
            for (byte b : digest) {
                shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            log.error("生成JsAPI签名失败1", e);
            log.error("生成JsAPI签名失败2 , " + data.toString());
            return null;
        }
    }
}
