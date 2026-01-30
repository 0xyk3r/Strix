package cn.projectan.strix.core.aop.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Map;
import java.util.Set;

/**
 * 敏感 Map 序列化器
 * <p>
 * 用于对 Map 类型数据中的敏感键进行脱敏处理，适用于 GET 请求参数等场景
 * </p>
 *
 * @author ProjectAn
 * @since 2024/1/30
 */
@SuppressWarnings("rawtypes")
public class SensitiveMapSerializer extends StdSerializer<Map> {

    /**
     * 默认敏感字段关键词
     */
    private static final Set<String> DEFAULT_SENSITIVE_KEYWORDS = Set.of(
            "password",
            "secret",
            "token",
            "credential",
            "authorization",
            "apikey",
            "accesskey",
            "secretkey",
            "privatekey"
    );

    /**
     * 脱敏后的替换值
     */
    private static final String MASKED_VALUE = "******";

    private final Set<String> sensitiveKeywords;

    public SensitiveMapSerializer() {
        super(Map.class);
        this.sensitiveKeywords = DEFAULT_SENSITIVE_KEYWORDS;
    }

    public SensitiveMapSerializer(Set<String> sensitiveKeywords) {
        super(Map.class);
        this.sensitiveKeywords = sensitiveKeywords;
    }

    @Override
    public void serialize(Map map, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();

        for (Object entryObj : map.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (key == null) {
                continue;
            }

            String keyStr = key.toString();
            gen.writeName(keyStr);

            if (value == null) {
                gen.writeNull();
            } else if (isSensitiveField(keyStr.toLowerCase())) {
                gen.writeString(MASKED_VALUE);
            } else if (value instanceof Map<?, ?> nestedMap) {
                serialize(nestedMap, gen, ctxt);
            } else if (value instanceof String strValue) {
                gen.writeString(strValue);
            } else if (value instanceof Number numValue) {
                gen.writeNumber(numValue.toString());
            } else if (value instanceof Boolean boolValue) {
                gen.writeBoolean(boolValue);
            } else if (value instanceof String[] arrValue) {
                gen.writeStartArray();
                for (String item : arrValue) {
                    gen.writeString(item);
                }
                gen.writeEndArray();
            } else {
                ctxt.writeValue(gen, value);
            }
        }

        gen.writeEndObject();
    }

    /**
     * 判断字段是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName) {
        for (String keyword : sensitiveKeywords) {
            if (fieldName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

}
