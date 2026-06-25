package cn.projectan.strix.core.module.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * AI 文件预览签名工具
 * <p>为文件预览 URL 生成 HMAC-SHA256 签名，支持时效验证。
 */
@Component
public class AiFilePreviewSigner {

    @Value("${strix.ai.preview-sign-secret}")
    private String secret;

    private static final long TTL_SECONDS = 600;

    public String generatePreviewUrl(String fileId) {
        long expire = System.currentTimeMillis() / 1000 + TTL_SECONDS;
        String sign = computeSign(fileId, expire);
        return "/api/system/ai/file/" + fileId + "/preview?sign=" + sign + "&expire=" + expire;
    }

    public boolean verifySign(String fileId, String sign, long expire) {
        if (System.currentTimeMillis() / 1000 > expire) {
            return false;
        }
        String expected = computeSign(fileId, expire);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    private String computeSign(String fileId, long expire) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((fileId + "|" + expire).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Sign computation failed", e);
        }
    }
}
