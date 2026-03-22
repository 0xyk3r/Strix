package cn.projectan.strix.core.encrypt;

import cn.hutool.crypto.symmetric.SM4;
import cn.projectan.strix.model.properties.system.StrixProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 字段加密解密服务
 * <p>
 * 使用 SM4/CBC/PKCS7Padding 算法进行加密解密，密钥通过 {@link StrixProperties} 配置。
 *
 * @author ProjectAn
 * @since 2026/01/29 02:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldEncryptUtil {

    private static final String SM4_PREFIX = "SM4:";
    private static final String IV = "StrixFieldCrypt!";

    private final StrixProperties strixProperties;

    private SM4 sm4;

    @PostConstruct
    private void init() {
        String key = strixProperties.getEncrypt().getField().getKey();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] ivBytes = IV.getBytes(StandardCharsets.UTF_8);
        sm4 = new SM4("CBC", "PKCS7Padding", keyBytes, ivBytes);
    }

    /**
     * 加密字符串（使用 SM4）
     *
     * @param plainText 明文
     * @return 密文（带 SM4: 前缀），如果输入为 null 则返回 null
     */
    public String encrypt(String plainText) {
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
     * 解密字符串
     *
     * @param cipherText 密文（带 SM4: 前缀）
     * @return 明文，如果输入为 null 则返回 null
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!isEncrypted(cipherText)) {
            log.warn("Strix Encrypt: 识别到未被加密的明文信息, 已直接返回.");
            return cipherText;
        }
        try {
            String encrypted = cipherText.substring(SM4_PREFIX.length());
            return sm4.decryptStr(encrypted, StandardCharsets.UTF_8);
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
        return text != null && text.startsWith(SM4_PREFIX);
    }

}
