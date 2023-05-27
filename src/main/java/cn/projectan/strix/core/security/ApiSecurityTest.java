package cn.projectan.strix.core.security;

import cn.projectan.strix.utils.ApiSignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        // 待加密的数据 （服务端响应给客户端）
        // String encrypt = "";
        String encrypt = objectMapper.writeValueAsString(map);
        encrypt = encrypt.replace("\n", "");
        encrypt = encrypt.replace(" ", "");

        // 待解密的数据 （客户端发来的请求）
        // String decrypt = "";
        String decrypt = "{\"data\":\"54921f2dac8714448790c00edcb29d2e9cb20828a1d0d35798e0a3b6edb00d86b316933b751b1375657ca4269664a1fa978eec2427a8e36b371002aa6912ae4e3fa0f23d1e0df336ac03d75d8109e74b50648d202072b90b248b2c080ef4ced0c069a70d310409c0ee3ae4a9d87a6f7b\",\"sign\":\"KqaGASMThXQ5hcZG3mZnBqUGrvw2YZnwspRP9nrIp0zYh5XXjW7Nu60AfTfmaxpI0Bcr7bvwY+QguIPCEFzTX7LUQlaK5DIAlAQ/IXvyTTWu+DZ163uWVV9ZpphUJkLnbDWg+O7P9gs/8RGQ8vRME3BFeMwuRh88ESAWfvF3RjM=\"}\n";
        decrypt = decrypt.replace("\n", "");
        decrypt = decrypt.replace(" ", "");

        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<>() {
        });
        System.out.println("===================使用客户端公钥加密（服务端响应给客户端）===================");
        System.out.println(objectMapper.writeValueAsString(apiSecurity.encrypt(encryptMap)));

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
