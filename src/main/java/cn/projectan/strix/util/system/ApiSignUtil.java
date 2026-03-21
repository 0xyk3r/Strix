package cn.projectan.strix.util.system;

import cn.hutool.crypto.SmUtil;
import cn.projectan.strix.config.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.util.Map;

/**
 * API 签名工具类
 * <p>
 * POST 请求：对原始请求体字符串签名，避免 DTO 序列化差异导致的签名不一致。
 * GET 请求：对排序后的查询参数 JSON 签名。
 * 签名算法：SM3（国密）
 *
 * @author ProjectAn
 * @since 2025/3/20 23:50
 */
@Slf4j
@Component
public class ApiSignUtil {

    private static final String SIGN_SEPARATOR = "|";

    private final ObjectMapper objectMapper;

    public ApiSignUtil() {
        // 基于全局基础 Jackson 配置增加字段排序功能
        objectMapper = JacksonConfig.builder()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }

    /**
     * 基于原始请求体字符串验证签名（POST 请求推荐）
     *
     * @param rawBody   原始请求体字符串（解密后的明文 JSON）
     * @param url       请求 URL
     * @param timestamp 时间戳
     * @param sign      客户端提供的签名
     * @return 验证结果
     */
    public boolean verifySign(String rawBody, String url, String timestamp, String sign) {
        String correctSign = getSign(rawBody, url, timestamp);
        return StringUtils.hasText(sign) && StringUtils.hasText(correctSign) && correctSign.equals(sign);
    }

    /**
     * 基于原始请求体字符串生成签名
     *
     * @param rawBody   原始请求体字符串
     * @param url       请求 URL
     * @param timestamp 时间戳
     * @return SM3 签名
     */
    public String getSign(String rawBody, String url, String timestamp) {
        String content = (rawBody != null ? rawBody : "") + SIGN_SEPARATOR + url + SIGN_SEPARATOR + timestamp;
        return SmUtil.sm3(content);
    }

    /**
     * 基于参数 Map 验证签名（GET 请求使用）
     *
     * @param params    排序后的参数 Map
     * @param url       请求 URL
     * @param timestamp 时间戳
     * @param sign      客户端提供的签名
     * @return 验证结果
     */
    public boolean verifySignFromParams(Map<String, Object> params, String url, String timestamp, String sign) {
        try {
            params.entrySet().removeIf(entry -> entry.getValue() == null);
            String json = objectMapper.writeValueAsString(params);
            String content = json + SIGN_SEPARATOR + url + SIGN_SEPARATOR + timestamp;
            String correctSign = SmUtil.sm3(content);
            return StringUtils.hasText(sign) && correctSign.equals(sign);
        } catch (Exception e) {
            log.error("GET 请求签名校验异常", e);
            return false;
        }
    }

}
