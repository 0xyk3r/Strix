package cn.projectan.strix.core.security;

import cn.projectan.strix.utils.ApiSignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author ProjectAn
 * @date 2021/5/7 20:00
 */
public class ApiSecurityTest {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        ApiSecurity apiSecurity = new ApiSecurity(objectMapper);

        String timestamp = String.valueOf(System.currentTimeMillis());

        // 待签名数据
        Map<String, Object> map = new TreeMap<>();
        // api地址
        map.put("_requestUrl", "/v1/login");
        map.put("_timestamp", timestamp);
        // 参数
        map.put("loginName", "anjiongyi");
        map.put("loginPass", "An1212");

        System.out.println("===============TIMESTAMP===============");
        System.out.println(timestamp);
        System.out.println("=================SIGN=================");
        String sign = ApiSignUtil.getSign(map, objectMapper);
        System.out.println(sign);
        System.out.println("======================================");

//        // 待加密的数据 （服务端响应给客户端）
//        // String encrypt = "";
//        String encrypt = objectMapper.writeValueAsString(map);
//        encrypt = encrypt.replace("\n", "");
//        encrypt = encrypt.replace(" ", "");

        // 待解密的数据 （客户端发来的请求）
        // String decrypt = "";
        String decrypt = "{\"data\":\"e8638e699689dcce5d6640debd6e96051370a58df16f0b4140cf4876400f997911b456ba2acd43b4ce7d8114d42f1c37aa1b20c3796c2ac5b755b7f7b239b0b0bde39f505ab249daaad7ec8b181c269bb1197321b69738040d13c5d9e62cceb37ffef024481d652282963f4119f7a9c8\",\"sign\":\"D8u6ztCpWCEmWBQZSe+bRA1pVrn0iphR5Xi+O+mlITyJrcJlkxjlaIl2MIg7YpZ8udUBga34rspwqEofW70PdVKru8MD+qG6+UlIV6G/U6vmHvSOv66i/93rRir5Mpf7lVd1a7ZpztHWGei11VzilmCn9bdDHW3PPKDJVKiWUGk\\u003d\"}";
        decrypt = decrypt.replace("\n", "");
        decrypt = decrypt.replace(" ", "");

//        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<>() {
//        });
//        System.out.println("===================使用客户端公钥加密（服务端响应给客户端）===================");
//        System.out.println(objectMapper.writeValueAsString(apiSecurity.encrypt(encryptMap)));

        try {
            System.out.println("===================私钥解密（解密客户端请求）===================");
            String decryptByPrivateKey = apiSecurity.decrypt(decrypt);
            Map<String, Object> m1 = objectMapper.readValue(decryptByPrivateKey, new TypeReference<>() {
            });
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m1));
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

}
