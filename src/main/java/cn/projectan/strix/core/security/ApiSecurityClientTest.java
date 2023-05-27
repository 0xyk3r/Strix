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
        ApiSecurityClient apiSecurityClient = new ApiSecurityClient(objectMapper);

        String timestamp = String.valueOf(System.currentTimeMillis());

        // 待签名数据
        Map<String, Object> map = new TreeMap<>();
        // api地址
        map.put("_requestUrl", "/v1/login");
        map.put("_timestamp", timestamp);
        // 参数
        map.put("loginName", "anjiongyi");
        map.put("loginPass", "An1212");

        // 请求头
        String sign = ApiSignUtil.getSign(map, objectMapper);
        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ REQUEST HEADER ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        System.out.println("sign: " + sign);
        System.out.println("timestamp: " + timestamp);

        // 待加密数据 （客户端请求服务端）
        // String encrypt = "";
        String encrypt = objectMapper.writeValueAsString(map);
        encrypt = encrypt.replace("\n", "");
        encrypt = encrypt.replace(" ", "");

        // 待解密数据 （服务端响应客户端）
        // String decrypt = "";
        String decrypt = "{\"data\":\"241da837ef64118806e97b5c2ea885c1a243d6cb4b60aa467ba24a687321b2154db050dd90fa347557d749de1e9e8b06856b73486cf4075d3511ff3808c2eed3c0b2f0109ceb2f05641a4f03ac57ed3b9328bc0d4912eeb9a4a0776befe2d967001280a481039aca697867117ce30a8a\",\"sign\":\"fZay1xRM+0uMPoGuAJYnZMN93nRpfb7eqRL6Y26/59/d88oQkORCB41FR5BWNiNfNAg57qDPhs9IE4TbWGJ2Bx8j8dklxYcnodo5mEWqeUtSac9X0rpnyhzNlA0RO3oBnHhby/iETAHwxdZtTgeI+b6rRS42OveXPjvfZXNREQ0=\"}\n";
        decrypt = decrypt.replace("\n", "");
        decrypt = decrypt.replace(" ", "");

        Map<String, Object> encryptMap = objectMapper.readValue(encrypt, new TypeReference<>() {
        });

        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ REQUEST  BODY ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        System.out.println(objectMapper.writeValueAsString(apiSecurityClient.encrypt(encryptMap)));

        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★ 解 密 响 应 体 ☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");
        String decryptByPublicKey = apiSecurityClient.decrypt(decrypt);
        Map<String, Object> m2 = objectMapper.readValue(decryptByPublicKey, new TypeReference<>() {
        });
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m2));
        System.out.println("☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★☆★");

    }

}
