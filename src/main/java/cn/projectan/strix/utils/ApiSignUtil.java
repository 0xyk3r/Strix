package cn.projectan.strix.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/6/10 16:20
 */
@Slf4j
public class ApiSignUtil {

    private static ObjectMapper OBJECT_MAPPER = null;

    /**
     * @param params 需要进行排序加密的参数
     * @return 验证签名结果 为null则为异常
     */
    public static boolean verifySign(Map<String, Object> params, String timestamp, String sign) {
        String trueSign = getSign(params, timestamp);
        return StringUtils.hasText(sign) && StringUtils.hasText(trueSign) && trueSign.equals(sign) && verifyTimestamp(timestamp);
    }

    public static boolean verifyTimestamp(String timestamp) {
        return StringUtils.hasText(timestamp) && System.currentTimeMillis() - Long.parseLong(timestamp) < 1000 * 30;
    }

    /**
     * @param params 需要进行排序加密的参数
     * @return 签名
     */
    public static String getSign(Map<String, Object> params, String timestamp) {
        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = SpringUtil.getBean(ObjectMapper.class);
        }
        return getSign(params, timestamp, OBJECT_MAPPER);
    }

    /**
     * @param params 需要进行排序加密的参数
     * @return 签名
     */
    public static String getSign(Map<String, Object> params, String timestamp, ObjectMapper objectMapper) {
        // 移除空参数
        params.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        params.put("_timestamp", timestamp);
        String paramsJsonStr;
        try {
            paramsJsonStr = objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("获取参数Sign时发生异常", e);
            return null;
        }
//        AES aes = new AES("CBC", "PKCS7Padding", ("fUCkUon" + timestamp + "T1me").getBytes(StandardCharsets.UTF_8), ApiSecurity.AES_IV.getBytes(StandardCharsets.UTF_8));
//        return aes.encryptHex(paramsJsonStr);
        return DigestUtil.md5Hex(paramsJsonStr);
    }

}
