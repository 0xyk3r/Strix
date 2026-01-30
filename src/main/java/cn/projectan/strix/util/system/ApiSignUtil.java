package cn.projectan.strix.util.system;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.projectan.strix.config.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.util.Map;

/**
 * API 签名工具类
 *
 * @author ProjectAn
 * @since 2021/6/10 16:20
 */
@Slf4j
@Component
public class ApiSignUtil {

    private final ObjectMapper objectMapper;

    public ApiSignUtil() {
        // 基于全局基础 Jackson 配置增加字段排序功能
        objectMapper = JacksonConfig.builder()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }

    /**
     * 验证签名
     *
     * @param params 需要进行排序加密的参数
     * @return 验证签名结果
     */
    public boolean verifySign(Map<String, Object> params, String sign) {
        String correctSign = getSign(params);
        return StringUtils.hasText(sign) && StringUtils.hasText(correctSign) && correctSign.equals(sign);
    }

    /**
     * 获取签名
     *
     * @param params 需要进行排序加密的参数
     * @return 签名
     */
    public String getSign(Map<String, Object> params) {
        // 移除空参数
        params.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        try {
            String json = objectMapper.writeValueAsString(params);
            return DigestUtil.md5Hex(json);
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
