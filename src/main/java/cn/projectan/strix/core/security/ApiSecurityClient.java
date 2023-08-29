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
@Component
public class ApiSecurityClient {

    private final ObjectMapper objectMapper;

    private static final String AES_IV = "fuCkUCrAck32fUcK";

    /**
     * 客户端 -> 服务端 用 服务端公钥加密
     */
    private static final String SERVER_RSA_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCIXjpS5HSOeAauqi/3j9R4X7lbLfClo+CSO0yDsGdTsWHgpjE8l96dqsNay7xSKNDKvJCDId9aLIRhUVUDuV+ad6g3jNKW0ywiFHXobMPusDS8Jab18QE0N/JDCzh+5MejQb+ccwWvWcOwXJevgemMqpXXq2rpAfwigl+sYxi8BwIDAQAB";

    /**
     * 客户端 <- 服务端 用 客户端私钥解密
     */
    private static final String CLIENT_RSA_PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBANBprui2mHZG6WvJxuWatDrMIhUecappAMF0h3IofTfisMQg11H1hsfDOm75qKlTPgwIa6BiL1VXndO4hg/q7ICrxjOOIw4cRWfTsCxepeA+RYlh1ZMIYJCWvmOEU+PY/F/SDbTyMrJ8OTsuEJ08eLIwnWXwbi3mUEXkAmFao3X7AgMBAAECgYAx34h2sfNsIm4LWD7bhRjqFR121lE3CWef48Xh4KSOchYA6Sb9uvak6SgblGzzEDOB56XxvG09S/k9yCN0vbAYb+370Yhz0HhajKbjifVPZuAjvCO9cdWxJWeWlAtGMdgMyG1PxTlhamPfI/YvvjQxpf9845lGwXsBaTrGwbSLOQJBAOx3tJCnMPvajzcODwud67YKnuCwISK4XSNj7K+TPH66j53JycTjD+e7eVaa4j2TdP85t8zYO4q3oEut30nyXt0CQQDhoLzMaUg1nmUoAv2o0CO8hYEIzWc6gP4STHmtZjOqqYWhjInMk3slfMTllzLBEDy/womSuqb68wPXscvrtt63AkEAyjj81BAHFfstKtn9B+Q/peijQmedjsG39QIJcYUq4P3OwBPHV3cPLQ/ojqXaAOrPzUyg4K+zC8hJby78m5KIiQJASF7DUBmQ9MnajmvvKt+gJs73pXgk3UoUtI/dE3ZNqjb3yuqGJJ1Fia+shCvsNqrboXJnqC3Ac4vRNrUrwG6GnwJAAlZK9BZg2ORZ9vSGg0+Ah0Ji7BSaozJUFEXtZ+A2cU3S+LN8/4aOaAhFECvXjTjJYddffMhXnqJ6/z5DvxncnQ==";

    public ApiSecurityClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 加密数据
     *
     * @param body 客户端请求体对象，一般为 Map
     * @return 加密后的客户端请求体 Map
     */
    public Map<String, String> encrypt(Object body) {
        try {
            return encrypt(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            log.error("加密数据时出现异常：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 加密数据
     *
     * @param bodyStr 客户端请求体字符串
     * @return 加密后的客户端请求体 Map
     */
    public Map<String, String> encrypt(String bodyStr) {
        try {
            // 生成随机 AES 秘钥
            byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
            String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
            // 对 AES 秘钥使用 RSA 私钥加密
            RSA rsa = new RSA(null, ApiSecurityClient.SERVER_RSA_PUBLIC_KEY);
            byte[] encryptedByte = rsa.encrypt(aesKey, KeyType.PublicKey);
            String encrypted = cn.hutool.core.codec.Base64.encode(encryptedByte);
            // 使用 AES 秘钥对实际数据进行加密
            AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurityClient.AES_IV.getBytes(StandardCharsets.UTF_8));
            String data = aes.encryptHex(bodyStr);
            // 组装数据
            Map<String, String> map = new HashMap<>();
            encrypted = encrypted.replaceAll(System.lineSeparator(), "");
            data = data.replaceAll(System.lineSeparator(), "");
            map.put("sign", encrypted);
            map.put("data", data);
            return map;
        } catch (Exception e) {
            log.error("加密数据时出现异常：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解密数据
     *
     * @param body 服务端响应体字符串
     * @return 解密后的服务端响应体字符串
     */
    public String decrypt(String body) {
        try {
            Map<String, String> bodyMap = objectMapper.readValue(body, new TypeReference<>() {
            });
            return decrypt(bodyMap);
        } catch (Exception e) {
            log.error("解密数据时出现异常：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解密数据
     *
     * @param bodyMap 服务端响应体 Map
     * @return 解密后的服务端响应体字符串
     */
    public String decrypt(Map<String, String> bodyMap) {
        try {
            // 从请求结果中获取加密数据和加密签名
            String data = bodyMap.get("data");
            String sign = bodyMap.get("sign");
            if (StringUtils.hasText(data) && StringUtils.hasText(sign)) {
                // 使用 RSA 公钥解密出加密的 AES 秘钥
                RSA rsa = new RSA(ApiSecurityClient.CLIENT_RSA_PRIVATE_KEY, null);
                byte[] decrypt = rsa.decrypt(Base64.getDecoder().decode(sign), KeyType.PrivateKey);
                // 使用 AES 秘钥对数据进行解密
                AES aes = new AES("CBC", "PKCS7Padding", decrypt, ApiSecurityClient.AES_IV.getBytes(StandardCharsets.UTF_8));
                return aes.decryptStr(data, CharsetUtil.CHARSET_UTF_8);
            }
        } catch (Exception e) {
            log.error("解密数据时出现异常：" + e.getMessage(), e);
        }
        return null;
    }

}
