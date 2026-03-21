package cn.projectan.strix.core.captcha.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.symmetric.SM4;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Strix Captcha SM4 加密工具类
 * <p>
 * 使用 SM4/ECB/PKCS5Padding 加密验证码坐标数据。
 * 密钥为 16 字节随机数，以 32 位 hex 字符串形式传输。
 * 加解密数据均为 hex 编码。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/21
 */
public final class StrixCaptchaSM4Util {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private StrixCaptchaSM4Util() {
    }

    /**
     * 生成随机 SM4 密钥（16 字节，返回 32 位 hex 字符串）
     */
    public static String getKey() {
        byte[] key = new byte[16];
        SECURE_RANDOM.nextBytes(key);
        return HexUtil.encodeHexStr(key);
    }

    /**
     * SM4/ECB 加密
     *
     * @param content 明文内容
     * @param hexKey  hex 格式密钥（32 位 hex = 16 字节）
     * @return hex 格式密文
     */
    public static String encrypt(String content, String hexKey) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(hexKey)) {
            return content;
        }
        byte[] keyBytes = HexUtil.decodeHex(hexKey);
        SM4 sm4 = new SM4(cn.hutool.crypto.Mode.ECB, cn.hutool.crypto.Padding.PKCS5Padding, keyBytes);
        return sm4.encryptHex(content, StandardCharsets.UTF_8);
    }

    /**
     * SM4/ECB 解密
     *
     * @param hexCiphertext hex 格式密文
     * @param hexKey        hex 格式密钥（32 位 hex = 16 字节）
     * @return 解密后的明文
     */
    public static String decrypt(String hexCiphertext, String hexKey) {
        if (!StringUtils.hasText(hexCiphertext) || !StringUtils.hasText(hexKey)) {
            return hexCiphertext;
        }
        byte[] keyBytes = HexUtil.decodeHex(hexKey);
        SM4 sm4 = new SM4(cn.hutool.crypto.Mode.ECB, cn.hutool.crypto.Padding.PKCS5Padding, keyBytes);
        return sm4.decryptStr(hexCiphertext, StandardCharsets.UTF_8);
    }

}
