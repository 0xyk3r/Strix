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
 * api加解密工具，服务端使用
 * <p>
 * 使用国密 SM2 + SM4 算法
 * SM2 用于加密传输 SM4 对称密钥，SM4 用于加密实际数据。
 * </p>
 *
 * @author ProjectAn
 * @since 2025/3/20 22:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSecurity {

    private final ObjectMapper objectMapper;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 服务端 SM2 私钥（用于解密客户端发来的数据）
     */
    public static final String SERVER_SM2_PRIVATE_KEY = "0b416a279ba883e97a9b3c02f8897a431102de7a8f196ab9421bf5b925c0b06b";

    /**
     * 客户端 SM2 公钥（用于加密发给客户端的数据）
     */
    public static final String CLIENT_SM2_PUBLIC_KEY = "04f21a55780355bf48a7227925dceb83695ba5b6287f2d31334b5166a69afa4e760b27c3c44220176e703b16c9ccd6b0741c43625fdf8087f378878330529847a7";

    /**
     * 加密响应数据（服务端 -> 客户端）
     * <p>
     * 1. 生成随机 SM4 密钥和 IV
     * 2. 使用客户端 SM2 公钥加密 SM4 密钥
     * 3. 使用 SM4/CBC 加密实际数据
     * 4. 返回 {sign, data, iv}
     * </p>
     */
    public Object encrypt(Object body) {
        try {
            String result = objectMapper.writeValueAsString(body);

            // 生成随机 SM4 密钥（16 字节）
            byte[] sm4KeyBytes = SecureUtil.generateKey("SM4").getEncoded();
            String sm4KeyBase64 = Base64.getEncoder().encodeToString(sm4KeyBytes);

            // 生成随机 IV（16 字节）
            byte[] ivBytes = SecureUtil.generateKey("SM4").getEncoded();
            String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);

            // 使用客户端 SM2 公钥加密 SM4 密钥
            SM2 sm2 = SmUtil.sm2(null, CLIENT_SM2_PUBLIC_KEY);
            byte[] encryptedKey = sm2.encrypt(sm4KeyBase64.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
            String sign = Base64.getEncoder().encodeToString(encryptedKey);

            // 使用 SM4/CBC 加密数据
            SM4 sm4 = new SM4(cn.hutool.crypto.Mode.CBC, cn.hutool.crypto.Padding.PKCS5Padding, sm4KeyBytes, ivBytes);
            String data = sm4.encryptHex(result, StandardCharsets.UTF_8);

            Map<String, String> map = new HashMap<>();
            map.put("sign", sign);
            map.put("data", data);
            map.put("iv", ivBase64);
            return map;
        } catch (Exception e) {
            log.error("加密数据时出现异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解密请求数据（客户端 -> 服务端）
     * <p>
     * 1. 使用服务端 SM2 私钥解密出 SM4 密钥
     * 2. 使用 SM4/CBC 解密实际数据
     * </p>
     */
    public String decrypt(String body) {
        String content = null;
        try {
            Map<String, String> map = objectMapper.readValue(body, new TypeReference<>() {
            });
            String data = map.get("data");
            String sign = map.get("sign");
            String iv = map.get("iv");

            if (StringUtils.hasText(data) && StringUtils.hasText(sign) && StringUtils.hasText(iv)) {
                // 使用服务端 SM2 私钥解密出 SM4 密钥
                SM2 sm2 = SmUtil.sm2(SERVER_SM2_PRIVATE_KEY, null);
                byte[] sm4KeyBase64Bytes = sm2.decrypt(Base64.getDecoder().decode(sign), KeyType.PrivateKey);
                byte[] sm4KeyBytes = Base64.getDecoder().decode(sm4KeyBase64Bytes);
                byte[] ivBytes = Base64.getDecoder().decode(iv);

                // 使用 SM4/CBC 解密数据
                SM4 sm4 = new SM4(cn.hutool.crypto.Mode.CBC, cn.hutool.crypto.Padding.PKCS5Padding, sm4KeyBytes, ivBytes);
                content = sm4.decryptStr(data, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：{}", body, e);
        }
        return content;
    }

}
