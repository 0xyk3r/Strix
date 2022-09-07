package cn.projectan.strix.core.security;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/5/7 20:00
 */
public class ApiSecurityTest {

    public static void main(String[] args) throws Exception {
//        aes();
//        System.out.println("-----------------------");
//        rsa();
//        System.out.println("-----------------------");
//        Map<String, Object> map = new HashMap<>();
//        map.put("abc", "123accACCh哈哈哈");
//        System.out.println(genRequest(map));
        ObjectMapper objectMapper = new ObjectMapper();
        ApiSecurity apiSecurity = new ApiSecurity(objectMapper);

        String encrypt = "{\"loginName\":\"anjiongyi\",\"loginPassword\":\"An1212\"}";
        // String decrypt = "";
        // String decrypt = "";
        String decrypt = "{ \"data\": \"8c457210a4db771eebfab480c09a5de35b176ba4421cdd2b31391b997b7e96ad6dd1cffe371437d8e4c4922a7f52042a\", \"sign\": \"SMfptSkWqjTj+uKcIwMfhqhZgA38qK4I4Waf409xWFqYeM7J2xQq4Bhbb2bSYcQS45hnV0ejt6MkBabbv0lOSRYPpgtJ7Jubxswj/LZPMZI84Az1Y/dXYhpdKeIlGwO6fSOo9CEYZpFIbSRLuyT9Y3e5AAz0QZ7bFGS6hP5iwAI=\" }";

        decrypt = decrypt.replaceAll(" ", "");
        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<Map<String, Object>>() {
        });

        System.out.println("===================私钥加密（服务端响应）===================");
        System.out.println(objectMapper.writeValueAsString(apiSecurity.encryptByPrivateKey(encryptMap)));
        System.out.println("===================公钥加密（客户端请求）===================");
        System.out.println(objectMapper.writeValueAsString(apiSecurity.encryptByPublicKey(encryptMap)));

        try {
            System.out.println("===================私钥解密（解密客户端请求）===================");
            String decryptByPrivateKey = apiSecurity.decryptByPrivateKey(decrypt);
            Map<String, Object> m1 = objectMapper.readValue(decryptByPrivateKey, new TypeReference<Map<String, Object>>() {
            });
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m1));
        } catch (Throwable e) {

        }

        try {
            System.out.println("===================公钥解密（解密服务端响应）===================");
            String decryptByPublicKey = apiSecurity.decryptByPublicKey(decrypt);
            Map<String, Object> m2 = objectMapper.readValue(decryptByPublicKey, new TypeReference<Map<String, Object>>() {
            });
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m2));
        } catch (Throwable e) {

        }

    }

    private static void aes() {
        String content = "test中文";

        // 生成随机AES秘钥
        byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);

        AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes());

        byte[] encrypt = aes.encrypt(content);
        byte[] decrypt = aes.decrypt(encrypt);

        String encryptHex = aes.encryptHex(content);
        String decryptStr = aes.decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);

        System.out.println(encryptHex);
        System.out.println(decryptStr);
    }

    private static void rsa() {
        RSA rsa = new RSA(ApiSecurity.RSA_PRIVATE_KEY, ApiSecurity.RSA_PUBLIC_KEY);

        // 公钥加密，私钥解密
        byte[] encrypt = rsa.encrypt(StrUtil.bytes("我是一段测试1234", CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
        System.out.println(Base64.getEncoder().encodeToString(encrypt));
        byte[] decrypt = rsa.decrypt(encrypt, KeyType.PrivateKey);
        System.out.println(StrUtil.str(decrypt, CharsetUtil.CHARSET_UTF_8));

        // 私钥加密，公钥解密
        byte[] encrypt2 = rsa.encrypt(StrUtil.bytes("我是一段测试4321", CharsetUtil.CHARSET_UTF_8), KeyType.PrivateKey);
        System.out.println(Base64.getEncoder().encodeToString(encrypt2));
        byte[] decrypt2 = rsa.decrypt(encrypt2, KeyType.PublicKey);
        System.out.println(StrUtil.str(decrypt2, CharsetUtil.CHARSET_UTF_8));

    }

    private static String genRequest(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String result = objectMapper.writeValueAsString(object);
            // 生成AES秘钥
            byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
            String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
            System.out.println("aesKey: " + aesKey);
            // 使用RSA公钥加密AES秘钥
            RSA rsa = new RSA(null, ApiSecurity.RSA_PUBLIC_KEY);
            byte[] encryptedByte = rsa.encrypt(aesKey, KeyType.PublicKey);
            String encrypted = Base64.getEncoder().encodeToString(encryptedByte);
            System.out.println(encrypted);
            // aes加密
            AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes());
            String data = aes.encryptHex(result);
            Map<String, String> map = new HashMap<>();
            map.put("sign", encrypted);
            map.put("data", data);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }

}
