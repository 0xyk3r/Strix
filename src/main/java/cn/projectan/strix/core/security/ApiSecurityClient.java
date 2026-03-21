package cn.projectan.strix.core.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * api加解密工具，客户端使用
 * <p>
 * 使用国密 SM2 + SM4 算法，与 ApiSecurity（服务端）配合使用。
 * </p>
 *
 * @author ProjectAn
 * @since 2025/3/20 22:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSecurityClient {

    private final ObjectMapper objectMapper;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 服务端 SM2 公钥（客户端用于加密发送给服务端的数据）
     */
    private static final String SERVER_SM2_PUBLIC_KEY = "0453b166e6c4249055c327b33472e580368c8b2800c81a63d21af59dcc6c82170f09c79ed62cab27b3f059e348aa3ad0c0db7add0856350acdb4f3f1749fb53f1f";

    /**
     * 客户端 SM2 私钥（客户端用于解密服务端返回的数据）
     */
    private static final String CLIENT_SM2_PRIVATE_KEY = "f1d9cbbe8cc06197f850967e3d6fd8e71b6ceab48d9dcbb89b0a248b9bed0569";

    /**
     * 加密请求数据（客户端 -> 服务端）
     */
    public Map<String, String> encrypt(Object body) {
        try {
            return encrypt(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            log.error("加密数据时出现异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 加密请求数据
     */
    public Map<String, String> encrypt(String bodyStr) {
        try {
            // 生成随机 SM4 密钥（16 字节）
            byte[] sm4KeyBytes = SecureUtil.generateKey("SM4").getEncoded();
            String sm4KeyBase64 = Base64.getEncoder().encodeToString(sm4KeyBytes);

            // 生成随机 IV（16 字节）
            byte[] ivBytes = SecureUtil.generateKey("SM4").getEncoded();
            String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);

            // 使用服务端 SM2 公钥加密 SM4 密钥
            SM2 sm2 = SmUtil.sm2(null, SERVER_SM2_PUBLIC_KEY);
            byte[] encryptedKey = sm2.encrypt(sm4KeyBase64.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
            String sign = Base64.getEncoder().encodeToString(encryptedKey);

            // 使用 SM4/CBC 加密数据
            SM4 sm4 = new SM4(cn.hutool.crypto.Mode.CBC, cn.hutool.crypto.Padding.PKCS5Padding, sm4KeyBytes, ivBytes);
            String data = sm4.encryptHex(bodyStr);

            Map<String, String> map = new HashMap<>();
            map.put("sign", sign.replaceAll(System.lineSeparator(), ""));
            map.put("data", data.replaceAll(System.lineSeparator(), ""));
            map.put("iv", ivBase64);
            return map;
        } catch (Exception e) {
            log.error("加密数据时出现异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解密响应数据（服务端 -> 客户端）
     */
    public String decrypt(String body) {
        try {
            Map<String, String> bodyMap = objectMapper.readValue(body, new TypeReference<>() {
            });
            return decrypt(bodyMap);
        } catch (Exception e) {
            log.error("解密数据时出现异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解密响应数据
     */
    public String decrypt(Map<String, String> bodyMap) {
        try {
            String data = bodyMap.get("data");
            String sign = bodyMap.get("sign");
            String iv = bodyMap.get("iv");
            if (StringUtils.hasText(data) && StringUtils.hasText(sign) && StringUtils.hasText(iv)) {
                // 使用客户端 SM2 私钥解密出 SM4 密钥
                SM2 sm2 = SmUtil.sm2(CLIENT_SM2_PRIVATE_KEY, null);
                byte[] sm4KeyBase64Bytes = sm2.decrypt(Base64.getDecoder().decode(sign), KeyType.PrivateKey);
                byte[] sm4KeyBytes = Base64.getDecoder().decode(sm4KeyBase64Bytes);
                byte[] ivBytes = Base64.getDecoder().decode(iv);

                // 使用 SM4/CBC 解密数据
                SM4 sm4 = new SM4(cn.hutool.crypto.Mode.CBC, cn.hutool.crypto.Padding.PKCS5Padding, sm4KeyBytes, ivBytes);
                return sm4.decryptStr(data, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：{}", e.getMessage(), e);
        }
        return null;
    }

}
