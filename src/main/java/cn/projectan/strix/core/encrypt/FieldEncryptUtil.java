package cn.projectan.strix.core.encrypt;

import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 字段加密解密工具类
 * <p>
 * 使用 AES/CBC/PKCS7Padding 算法进行加密解密，密钥和IV通过配置文件指定。
 *
 * @author ProjectAn
 * @since 2026/01/29 02:00
 */
@Slf4j
@Component
public class FieldEncryptUtil {

    /**
     * 加密数据前缀，用于标识数据已加密，避免重复加密
     */
    private static final String ENCRYPT_PREFIX = "ENC:";

    private static AES aes;

    /**
     * 初始化 AES 加密器
     *
     * @param key AES 密钥，必须是 16/24/32 字节
     */
    @Value("${strix.encrypt.field.key:Strix@FieldCrypt}")
    public void setKey(String key) {
        // 默认使用配置的 IV，如果不配置则使用固定值
        String iv = "StrixFieldCrypt!";
        aes = new AES("CBC", "PKCS7Padding",
                key.getBytes(StandardCharsets.UTF_8),
                iv.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return 密文（带前缀），如果输入为 null 则返回 null
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        // 如果已经加密过，直接返回
        if (isEncrypted(plainText)) {
            return plainText;
        }
        try {
            String encrypted = aes.encryptBase64(plainText, StandardCharsets.UTF_8);
            return ENCRYPT_PREFIX + encrypted;
        } catch (Exception e) {
            log.error("字段加密失败: {}", e.getMessage(), e);
            return plainText;
        }
    }

    /**
     * 解密字符串
     *
     * @param cipherText 密文（带前缀）
     * @return 明文，如果输入为 null 则返回 null
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        // 如果没有加密前缀，说明是明文，直接返回
        if (!isEncrypted(cipherText)) {
            log.warn("Strix Encrypt: 识别到未被加密的明文信息, 已直接返回.");
            return cipherText;
        }
        try {
            String encrypted = cipherText.substring(ENCRYPT_PREFIX.length());
            return aes.decryptStr(encrypted, StandardCharsets.UTF_8);
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
        return text != null && text.startsWith(ENCRYPT_PREFIX);
    }

}
