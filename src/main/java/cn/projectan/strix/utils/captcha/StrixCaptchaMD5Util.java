package cn.projectan.strix.utils.captcha;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


/**
 * Strix Captcha MD5 工具类
 *
 * @author ProjectAn
 * @date 2024/3/30 13:00
 */
@Slf4j
public abstract class StrixCaptchaMD5Util {

    /**
     * 获取指定字符串的md5值
     *
     * @param dataStr 明文
     * @return String
     */
    public static String md5(String dataStr) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes(StandardCharsets.UTF_8));
            byte[] s = m.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : s) {
                result.append(Integer.toHexString((0x000000FF & b) | 0xFFFFFF00).substring(6));
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("md5 error", e);
        }
        return "";
    }

    /**
     * 获取指定字符串的md5值, md5(str+salt)
     *
     * @param dataStr 明文
     * @return String
     */
    public static String md5WithSalt(String dataStr, String salt) {
        return md5(dataStr + salt);
    }

}
