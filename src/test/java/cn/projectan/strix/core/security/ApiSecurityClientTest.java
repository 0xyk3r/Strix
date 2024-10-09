package cn.projectan.strix.core.security;

import cn.projectan.strix.util.ApiSignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author ProjectAn
 * @since 2024/4/9 下午5:21
 */
class ApiSecurityClientTest {

    @Test
    void test() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApiSecurityClient apiSecurityClient = new ApiSecurityClient(objectMapper);

        String timestamp = String.valueOf(System.currentTimeMillis());

        // 待签名数据
        Map<String, Object> map = new TreeMap<>();
        // api地址
        map.put("_requestUrl", "/v1/user/getVerifyCode/register");
        map.put("_timestamp", "1692190035334");
        // 参数
        map.put("loginName", "743730738@qq.com");

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
        String decrypt = "{\"data\":\"85316af9ac2c1a9b4a3deaf26624b414cf55f65353b8a026012a17cec243d6ac\",\"sign\":\"gZNhGR9WasSHae58DcFJG0i1WqvptTYjiCoQYPaVWndvD9SCMXyw2jpWn/ixYrVoCn9yEp4x+ix3Zuz08u99a2bViGkhssD4+6hAjRWZPeG0u48U0t7ZsyIsCzqfKmwzYlGOwARyfdPqdqFeRzP6PBEUJSVPmTZO7qGUSein73w=\"}";
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
