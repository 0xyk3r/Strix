package cn.projectan.strix.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * API 签名工具类
 *
 * @author ProjectAn
 * @date 2021/6/10 16:20
 */
@Slf4j
public class ApiSignUtil {

    private static ObjectMapper OBJECT_MAPPER = null;

    /**
     * @param params 需要进行排序加密的参数
     * @return 验证签名结果 为null则为异常
     */
    public static boolean verifySign(Map<String, Object> params, String sign) {
        String correctSign = getSign(params);
        return StringUtils.hasText(sign) && StringUtils.hasText(correctSign) && correctSign.equals(sign);
    }

    /**
     * @param params 需要进行排序加密的参数
     * @return 签名
     */
    public static String getSign(Map<String, Object> params) {
        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = SpringUtil.getBean(ObjectMapper.class);
        }
        // 移除空参数
        params.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        try {
            return DigestUtil.md5Hex(OBJECT_MAPPER.writeValueAsString(params));
        } catch (Exception e) {
            log.error("获取参数Sign时发生异常", e);
            return null;
        }
    }

    /**
     * 仅供测试使用 使用传入的 ObjectMapper
     *
     * @param params       需要进行排序加密的参数
     * @param objectMapper ObjectMapper
     * @return 签名
     */
    public static String getSign(Map<String, Object> params, ObjectMapper objectMapper) {
        // 移除空参数
        params.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        try {
            return DigestUtil.md5Hex(objectMapper.writeValueAsString(params));
        } catch (Exception e) {
            log.error("获取参数Sign时发生异常", e);
            return null;
        }
    }

}
