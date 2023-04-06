package cn.projectan.strix.core.security;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * api加解密工具，客户端使用
 *
 * @author 安炯奕
 * @date 2023/4/6 17:10
 */
@Slf4j
public class ApiSecurityClient {

    private final ObjectMapper objectMapper;

    public static final String AES_IV = "fuCkUCrAck32fUcK";
    public static final String RSA_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCIXjpS5HSOeAauqi/3j9R4X7lbLfClo+CSO0yDsGdTsWHgpjE8l96dqsNay7xSKNDKvJCDId9aLIRhUVUDuV+ad6g3jNKW0ywiFHXobMPusDS8Jab18QE0N/JDCzh+5MejQb+ccwWvWcOwXJevgemMqpXXq2rpAfwigl+sYxi8BwIDAQAB";

    public ApiSecurityClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object encryptByPublicKey(Object body) {
        try {
            String result = objectMapper.writeValueAsString(body);
            // 生成随机AES秘钥
            byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
            String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
            // 对AES秘钥使用RSA公钥加密
            RSA rsa = new RSA(null, ApiSecurityClient.RSA_PUBLIC_KEY);
            byte[] encryptedByte = rsa.encrypt(aesKey, KeyType.PublicKey);
            String encrypted = Base64.getEncoder().encodeToString(encryptedByte);
            // 使用AES秘钥对实际数据进行加密
            AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), AES_IV.getBytes(StandardCharsets.UTF_8));
            String data = aes.encryptHex(result, StandardCharsets.UTF_8);
            // 组装返回
            Map<String, String> map = new HashMap<>();
            map.put("sign", encrypted);
            map.put("data", data);
            return map;
        } catch (Exception e) {
            log.error("加密数据时出现异常：" + e.getMessage(), e);
            return "ERROR";
        }
    }

    public String decryptByPublicKey(String body) {
        String content = null;
        try {
            Map<String, String> map = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {
            });
            // 从请求结果中获取加密数据和加密签名
            String data = map.get("data");
            String sign = map.get("sign");
            if (StringUtils.hasText(data) && StringUtils.hasText(sign)) {
                // 使用RSA公钥解密出加密的AES秘钥
                RSA rsa = new RSA(null, ApiSecurityClient.RSA_PUBLIC_KEY);
                byte[] decrypt = rsa.decrypt(Base64.getDecoder().decode(sign), KeyType.PublicKey);
                // 使用AES秘钥对实际数据进行解密
                AES aes = new AES("CBC", "PKCS7Padding", decrypt, ApiSecurityClient.AES_IV.getBytes(StandardCharsets.UTF_8));
                content = aes.decryptStr(data, CharsetUtil.CHARSET_UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：" + e.getMessage());
        }
        return content;
    }

}
