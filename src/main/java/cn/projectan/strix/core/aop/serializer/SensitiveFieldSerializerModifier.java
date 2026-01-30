package cn.projectan.strix.core.aop.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;
import java.util.Set;

/**
 * 敏感字段序列化修改器
 * <p>
 * 用于在日志记录时对敏感字段进行脱敏处理，将敏感字段值替换为 ******
 * </p>
 *
 * @author ProjectAn
 * @since 2024/1/30
 */
public class SensitiveFieldSerializerModifier extends ValueSerializerModifier {

    /**
     * 默认敏感字段关键词（不区分大小写，使用 contains 匹配）
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

    public SensitiveFieldSerializerModifier() {
        this.sensitiveKeywords = DEFAULT_SENSITIVE_KEYWORDS;
    }

    public SensitiveFieldSerializerModifier(Set<String> sensitiveKeywords) {
        this.sensitiveKeywords = sensitiveKeywords;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            String fieldName = writer.getName().toLowerCase();

            if (isSensitiveField(fieldName)) {
                beanProperties.set(i, new MaskedBeanPropertyWriter(writer));
            }
        }
        return beanProperties;
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

    /**
     * 脱敏属性写入器
     */
    private static class MaskedBeanPropertyWriter extends BeanPropertyWriter {

        private final BeanPropertyWriter delegate;

        public MaskedBeanPropertyWriter(BeanPropertyWriter base) {
            super(base);
            this.delegate = base;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            Object value;
            try {
                value = delegate.get(bean);
            } catch (Exception e) {
                value = null;
            }
            gen.writeName(delegate.getName());
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(MASKED_VALUE);
            }
        }
    }

}
