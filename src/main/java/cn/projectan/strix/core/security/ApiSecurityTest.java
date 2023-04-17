package cn.projectan.strix.core.security;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.projectan.strix.utils.ApiSignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author 安炯奕
 * @date 2021/5/7 20:00
 */
public class ApiSecurityTest {

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApiSecurity apiSecurity = new ApiSecurity(objectMapper);

        Map<String, Object> map = new TreeMap<>();
        map.put("_requestUrl", "/v1/login");
        map.put("loginName", "anjiongyi");
        map.put("loginPass", "An1212");

        System.out.println(System.currentTimeMillis());
        String sign = ApiSignUtil.getSign(map, String.valueOf(System.currentTimeMillis()), objectMapper);
        System.out.println("=================SIGN=================");
        System.out.println(sign);
        System.out.println("======================================");

        // String encrypt = "";
        String encrypt = objectMapper.writeValueAsString(map);
        encrypt = encrypt.replace("\n", "");
        encrypt = encrypt.replace(" ", "");
        System.out.println(encrypt);
        // String decrypt = "";
        String decrypt = "{\n" +
                "    \"data\": \"d92e4384e7865ffb438c74c2881b9846ec3e6da113af11253c443651735481201156661cafc0522256ffa0486314790a\",\n" +
                "    \"sign\": \"AyhA5J/z0mZmxQCCgwmaZOxQdrEZ65fn0ZEo5iDgtP4eZpzka2uJ1ZsJ18oK8w9b3qqKOt1LEZlCLF0j3i5k32JjrXDFNfUPmQq2K5QsiJqj93Ib0BGcrPhXViOaZ5NvBDXlzeiQ2zqUZ6LrPP75gGxsytrJ/XmjB9msuNykEVM=\"\n" +
                "}";
        decrypt = decrypt.replace("\n", "");
        decrypt = decrypt.replace(" ", "");

        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<Map<String, Object>>() {
        });

//        System.out.println("===================私钥加密（服务端响应）===================");
//        System.out.println(objectMapper.writeValueAsString(apiSecurity.encryptByPrivateKey(encryptMap)));
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

//        try {
//            System.out.println("===================公钥解密（解密服务端响应）===================");
//            String decryptByPublicKey = apiSecurity.decryptByPublicKey(decrypt);
//            Map<String, Object> m2 = objectMapper.readValue(decryptByPublicKey, new TypeReference<Map<String, Object>>() {
//            });
//            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m2));
//        } catch (Throwable e) {
//
//        }

    }

    private static void aes() {
        String timestamp = "1680781898948";
        String content = "{\"_requestUrl\":\"/v1/login\",\"loginName\":\"admin\",\"loginPass\":\"admin\"}";

        // 生成随机AES秘钥
//        byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
//        String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
//        AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes());

        AES aes = new AES("CBC", "PKCS7Padding", ("fUCkUon" + timestamp + "T1me").getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes(StandardCharsets.UTF_8));

        byte[] encrypt = aes.encrypt(content);
        byte[] decrypt = aes.decrypt(encrypt);

        String encryptHex = aes.encryptHex(content);
        String decryptStr = aes.decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);

        System.out.println("======AES======");
        System.out.println(encryptHex);
        System.out.println(decryptStr);
        System.out.println("===============");
    }

//    private static void rsa() {
//        RSA rsa = new RSA(ApiSecurity.SERVER_RSA_PRIVATE_KEY, ApiSecurity.SERVER_RSA_PUBLIC_KEY);
//
//        // 公钥加密，私钥解密
//        byte[] encrypt = rsa.encrypt(StrUtil.bytes("我是一段测试1234", CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
//        System.out.println(Base64.getEncoder().encodeToString(encrypt));
//        byte[] decrypt = rsa.decrypt(encrypt, KeyType.PrivateKey);
//        System.out.println(StrUtil.str(decrypt, CharsetUtil.CHARSET_UTF_8));
//
//        // 私钥加密，公钥解密
//        byte[] encrypt2 = rsa.encrypt(StrUtil.bytes("我是一段测试4321", CharsetUtil.CHARSET_UTF_8), KeyType.PrivateKey);
//        System.out.println(Base64.getEncoder().encodeToString(encrypt2));
//        byte[] decrypt2 = rsa.decrypt(encrypt2, KeyType.PublicKey);
//        System.out.println(StrUtil.str(decrypt2, CharsetUtil.CHARSET_UTF_8));
//    }

//    private static String genRequest(Object object) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//
//            String result = objectMapper.writeValueAsString(object);
//            // 生成AES秘钥
//            byte[] aesKeyBase64 = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
//            String aesKey = Base64.getEncoder().encodeToString(aesKeyBase64);
//            System.out.println("aesKey: " + aesKey);
//            // 使用RSA公钥加密AES秘钥
//            RSA rsa = new RSA(null, ApiSecurity.SERVER_RSA_PUBLIC_KEY);
//            byte[] encryptedByte = rsa.encrypt(aesKey, KeyType.PublicKey);
//            String encrypted = Base64.getEncoder().encodeToString(encryptedByte);
//            System.out.println(encrypted);
//            // aes加密
//            AES aes = new AES("CBC", "PKCS7Padding", aesKey.getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes());
//            String data = aes.encryptHex(result);
//            Map<String, String> map = new HashMap<>();
//            map.put("sign", encrypted);
//            map.put("data", data);
//            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "null";
//        }
//    }

}
