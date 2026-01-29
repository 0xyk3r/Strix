package cn.projectan.strix.util.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 工具类
 *
 * @author ProjectAn
 * @since 2026/1/29 09:59
 */
@Slf4j
public class ObjectMapperUtil {

    private ObjectMapperUtil() {
    }

    private static class Holder {
        private static final ObjectMapper INSTANCE = SpringUtil.getBean(ObjectMapper.class);
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper get() {
        return Holder.INSTANCE;
    }

    /**
     * 解析 JSON 字符串
     *
     * @param value     JSON 字符串
     * @param valueType 目标类型
     * @param <T>       目标类型
     * @return 目标对象
     */
    public static <T> T readValue(String value, Class<T> valueType) {
        try {
            return get().readValue(value, valueType);
        } catch (Exception e) {
            log.error("Strix JSON: 解析 JSON 失败. {}", value, e);
            return null;
        }
    }

    /**
     * 解析 JSON 字符串
     *
     * @param value        JSON 字符串
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型
     * @return 目标对象
     */
    public static <T> T readValue(String value, TypeReference<T> valueTypeRef) {
        try {
            return get().readValue(value, valueTypeRef);
        } catch (Exception e) {
            log.error("Strix JSON: 解析 JSON 失败. {}", value, e);
            return null;
        }
    }

    public static String writeValue(Object value) {
        try {
            return get().writeValueAsString(value);
        } catch (Exception e) {
            log.error("Strix JSON: 序列化对象为 JSON 失败. {}", value, e);
            return null;
        }
    }

}
