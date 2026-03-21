package cn.projectan.strix.core.encrypt;

import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SM4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 字段加密解密工具类
 * <p>
 * 使用 SM4/CBC/PKCS7Padding 算法进行加密解密，密钥和IV通过配置文件指定。
 * 兼容旧版 AES/CBC 加密数据的解密（"ENC:" 前缀），新数据统一使用 SM4（"SM4:" 前缀）。
 *
 * @author ProjectAn
 * @since 2026/01/29 02:00
 */
@Slf4j
@Component
public class FieldEncryptUtil {

    /**
     * 旧版 AES 加密数据前缀（兼容解密用）
     */
    private static final String LEGACY_AES_PREFIX = "ENC:";

    /**
     * SM4 加密数据前缀
     */
    private static final String SM4_PREFIX = "SM4:";

    private static SM4 sm4;

    /**
     * 旧版 AES 解密器，用于兼容历史数据
     */
    private static AES legacyAes;

    /**
     * 初始化 SM4 加密器及旧版 AES 解密器
     *
     * @param key 密钥，必须是 16 字节
     */
    @Value("${strix.encrypt.field.key:Strix@FieldCrypt}")
    public void setKey(String key) {
        String iv = "StrixFieldCrypt!";
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
        sm4 = new SM4("CBC", "PKCS7Padding", keyBytes, ivBytes);
        legacyAes = new AES("CBC", "PKCS7Padding", keyBytes, ivBytes);
    }

    /**
     * 加密字符串（使用 SM4）
     *
     * @param plainText 明文
     * @return 密文（带 SM4: 前缀），如果输入为 null 则返回 null
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }
        try {
            String encrypted = sm4.encryptBase64(plainText, StandardCharsets.UTF_8);
            return SM4_PREFIX + encrypted;
        } catch (Exception e) {
            log.error("字段加密失败: {}", e.getMessage(), e);
            return plainText;
        }
    }

    /**
     * 解密字符串（自动识别 SM4 或旧版 AES）
     *
     * @param cipherText 密文（带前缀）
     * @return 明文，如果输入为 null 则返回 null
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!isEncrypted(cipherText)) {
            log.warn("Strix Encrypt: 识别到未被加密的明文信息, 已直接返回.");
            return cipherText;
        }
        try {
            if (cipherText.startsWith(SM4_PREFIX)) {
                String encrypted = cipherText.substring(SM4_PREFIX.length());
                return sm4.decryptStr(encrypted, StandardCharsets.UTF_8);
            }
            if (cipherText.startsWith(LEGACY_AES_PREFIX)) {
                String encrypted = cipherText.substring(LEGACY_AES_PREFIX.length());
                return legacyAes.decryptStr(encrypted, StandardCharsets.UTF_8);
            }
            return cipherText;
        } catch (Exception e) {
            log.error("字段解密失败: {}", e.getMessage(), e);
            return cipherText;
        }
    }

    /**
     * 判断字符串是否已加密
     *
     * @param text 待检测字符串
     * @return 是否已加密
     */
    public static boolean isEncrypted(String text) {
        return text != null && (text.startsWith(SM4_PREFIX) || text.startsWith(LEGACY_AES_PREFIX));
    }

}
