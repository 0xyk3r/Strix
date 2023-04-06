package cn.projectan.strix.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.projectan.strix.core.security.ApiSecurityClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/6/10 16:20
 */
@Slf4j
public class ApiSignUtil {

    private static final ObjectMapper OBJECT_MAPPER = SpringUtil.getBean(ObjectMapper.class);

    /**
     * @param params 需要进行排序加密的参数
     * @return 验证签名结果 为null则为异常
     */
    public static boolean verifySign(Map<String, Object> params, String timestamp, String sign) {
        String trueSign = getSign(params, timestamp);
        return StringUtils.hasText(sign) && StringUtils.hasText(trueSign) && trueSign.equals(sign);
    }

    /**
     * @param params 需要进行排序加密的参数
     * @return 签名
     */
    public static String getSign(Map<String, Object> params, String timestamp) {
        // 移除空参数
        params.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        // 以下代码在循环过程中操作了Map，会抛出ConcurrentModificationException异常，故换成上方代码
//        for (Map.Entry<String, String> entry : params.entrySet()) {
//            if (!StringUtils.hasText(entry.getValue())) {
//                params.remove(entry.getKey());
//            }
//        }
        String paramsJsonStr;
        try {
            paramsJsonStr = OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("获取参数Sign时发生异常", e);
            return null;
        }
        AES aes = new AES("CBC", "PKCS7Padding", ("fUCkUon" + timestamp + "T1me").getBytes(StandardCharsets.UTF_8), ApiSecurityClient.AES_IV.getBytes(StandardCharsets.UTF_8));
        return aes.encryptHex(paramsJsonStr);
    }

}
