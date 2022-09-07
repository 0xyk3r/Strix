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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/5/2 18:10
 */
@Slf4j
@Component
public class ApiSecurity {

    private final ObjectMapper objectMapper;

    public static final String AES_SECRET_KEY = "fCKuOAUlDR0z/jQITtrBeQ==";
    public static final String AES_IV = "fuckyou0babyFUCK";
    public static final IvParameterSpec AES_IV_PARAMETER_SPEC = new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8));
    public static final String RSA_PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIheOlLkdI54Bq6qL/eP1HhfuVst8KWj4JI7TIOwZ1OxYeCmMTyX3p2qw1rLvFIo0Mq8kIMh31oshGFRVQO5X5p3qDeM0pbTLCIUdehsw+6wNLwlpvXxATQ38kMLOH7kx6NBv5xzBa9Zw7Bcl6+B6YyqlderaukB/CKCX6xjGLwHAgMBAAECgYAq0gPYcZpT/kaC5DfpscVTAyPuCK/vI1VqNaqiE2tusV19sFH3p+ykb7GmOiFpXx2o+6sZMjKzWxU6hdJ/N99X52DhPbt6bEwyAAQ5mpzW18H9+ABvZWpn5/LSj2xj/sAcDXZnSje/wXXDaSF/GZ39S/c7v8M6vgqdrk1K+CCxuQJBAOFXGjByc1lQg9nw4d0Uq2EU4q7ORIaNUUA14PhS40qgze6CPn2/+fS/s119ChUOy6XXJl3ss4SaQYjZeOSh2sUCQQCa7BuTBwFIFznpL1tkBxH9ckEpvTiKHVakQVvjXWLWWM6DcEL34loImxWg9digVF3/bS19bJAcXy7MrcgLr5hbAkAT55rDnsh7ojYTYUjCO5or2Clx4XyCGieMMXYu2TuEkxG9uLmGaBfPO8O/RVVHqOfqPUgBUfBFjU6upO8d2wI1AkEAkcnI9SZtbVL2G1uGbG4+3rvrWIUJtOeBBle/SgoynbW6uXQmgTFQOrL+updAQTjDsEAkw9grEZf86X5MN7sJ6wJAXBhG3chA4B6DVYbbs2+zpfIMYKd+s5kFzIVNVXlLaUrFkEwRlK56QHk+y63RpeUfAAueD4DxKu5wAV2sh5AxDQ==";
    public static final String RSA_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCIXjpS5HSOeAauqi/3j9R4X7lbLfClo+CSO0yDsGdTsWHgpjE8l96dqsNay7xSKNDKvJCDId9aLIRhUVUDuV+ad6g3jNKW0ywiFHXobMPusDS8Jab18QE0N/JDCzh+5MejQb+ccwWvWcOwXJevgemMqpXXq2rpAfwigl+sYxi8BwIDAQAB";

    public ApiSecurity(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object encryptByPrivateKey(Object body) throws Exception {
        String result = objectMapper.writeValueAsString(body);
        // 生成随机AES秘钥
        byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
        // 对AES秘钥使用RSA私钥加密
        RSA rsa = new RSA(ApiSecurity.RSA_PRIVATE_KEY, null);
        byte[] encryptedByte = rsa.encrypt(aesKey, KeyType.PrivateKey);
        String encrypted = Base64.getEncoder().encodeToString(encryptedByte);
        // 使用AES秘钥对实际数据进行加密
        AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes());
        String data = aes.encryptHex(result);
        // 组装返回
        Map<String, String> map = new HashMap<>();
        encrypted = encrypted.replaceAll(System.lineSeparator(), "");
        data = data.replaceAll(System.lineSeparator(), "");
        map.put("sign", encrypted);
        map.put("data", data);
        return map;

    }

    public Object encryptByPublicKey(Object body) {
        try {
            String result = objectMapper.writeValueAsString(body);
            // 生成随机AES秘钥
            byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
            String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
            // 对AES秘钥使用RSA公钥加密
            RSA rsa = new RSA(null, ApiSecurity.RSA_PUBLIC_KEY);
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

    public String decryptByPrivateKey(String body) {
        String content = null;
        try {
            Map<String, String> map = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {
            });
            String data = map.get("data");
            String sign = map.get("sign");

            if (StringUtils.hasText(data) && StringUtils.hasText(sign)) {
                RSA rsa = new RSA(ApiSecurity.RSA_PRIVATE_KEY, null);
                byte[] decrypt = rsa.decrypt(Base64.getDecoder().decode(sign), KeyType.PrivateKey);
                AES aes = new AES("CBC", "PKCS7Padding", decrypt, ApiSecurity.AES_IV.getBytes());
                content = aes.decryptStr(data, CharsetUtil.CHARSET_UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：" + e.getMessage());
        }
        return content;
    }


    public String decryptByPublicKey(String body) {
        String content = null;
        try {
            Map<String, String> map = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {
            });
            String data = map.get("data");
            String sign = map.get("sign");
            if (StringUtils.hasText(data) && StringUtils.hasText(sign)) {
                RSA rsa = new RSA(null, ApiSecurity.RSA_PUBLIC_KEY);
                byte[] decrypt = rsa.decrypt(Base64.getDecoder().decode(sign), KeyType.PublicKey);
                AES aes = new AES("CBC", "PKCS7Padding", decrypt, ApiSecurity.AES_IV.getBytes());
                content = aes.decryptStr(data, CharsetUtil.CHARSET_UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：" + e.getMessage());
        }
        return content;
    }

}
