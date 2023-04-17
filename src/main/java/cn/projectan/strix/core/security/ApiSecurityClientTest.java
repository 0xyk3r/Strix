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
public class ApiSecurityClientTest {

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApiSecurityClient apiSecurity = new ApiSecurityClient(objectMapper);

        Map<String, Object> map = new TreeMap<>();
        // api地址
        map.put("_requestUrl", "/v1/login");
        // 参数
        map.put("loginName", "anjiongyi");
        map.put("loginPass", "An1212");

        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = ApiSignUtil.getSign(map, timestamp, objectMapper);
        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ REQUEST HEADER ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        System.out.println("sign: " + sign);
        System.out.println("timestamp: " + timestamp);

        // 需要加密的客户端请求
        // String encrypt = "";
        String encrypt = objectMapper.writeValueAsString(map);
        encrypt = encrypt.replace("\n", "");
        encrypt = encrypt.replace(" ", "");

        // 需要解密的服务端响应
        // String decrypt = "";
        String decrypt = "{\n" +
                "    \"data\": \"d92e4384e7865ffb438c74c2881b9846ec3e6da113af11253c443651735481201156661cafc0522256ffa0486314790a\",\n" +
                "    \"sign\": \"AyhA5J/z0mZmxQCCgwmaZOxQdrEZ65fn0ZEo5iDgtP4eZpzka2uJ1ZsJ18oK8w9b3qqKOt1LEZlCLF0j3i5k32JjrXDFNfUPmQq2K5QsiJqj93Ib0BGcrPhXViOaZ5NvBDXlzeiQ2zqUZ6LrPP75gGxsytrJ/XmjB9msuNykEVM=\"\n" +
                "}";
        decrypt = decrypt.replace("\n", "");
        decrypt = decrypt.replace(" ", "");

        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<Map<String, Object>>() {
        });

        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ REQUEST  BODY ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        System.out.println(objectMapper.writeValueAsString(apiSecurity.encryptByPublicKey(encryptMap)));

        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ 解 密 响 应 体 ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        String decryptByPublicKey = apiSecurity.decryptByPublicKey(decrypt);
        Map<String, Object> m2 = objectMapper.readValue(decryptByPublicKey, new TypeReference<Map<String, Object>>() {
        });
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m2));
        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");

    }

}
