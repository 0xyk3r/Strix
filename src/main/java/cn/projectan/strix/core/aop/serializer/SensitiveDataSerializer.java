package cn.projectan.strix.core.aop.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Set;

/**
 * 敏感数据序列化器 - 用于密码字段脱敏
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
public class SensitiveDataSerializer extends StdSerializer<Object> {

    /**
     * 需要脱敏的字段名称（小写）
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "newpassword",
            "oldpassword",
            "confirmpassword",
            "loginpassword",
            "pwd",
            "secret",
            "token",
            "apikey",
            "accesstoken",
            "refreshtoken"
    );

    /**
     * 脱敏后的显示内容
     */
    private static final String MASKED_VALUE = "******";

    public SensitiveDataSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String fieldName = gen.getOutputContext().getCurrentName();

        if (fieldName != null && SENSITIVE_FIELDS.contains(fieldName.toLowerCase())) {
            gen.writeString(MASKED_VALUE);
        } else {
            // 正常序列化
            if (value == null) {
                gen.writeNull();
            } else {
                JsonSerializer<Object> serializer = provider.findValueSerializer(value.getClass());
                serializer.serialize(value, gen, provider);
            }
        }
    }

    /**
     * 判断字段名是否为敏感字段
     */
    public static boolean isSensitiveField(String fieldName) {
        return fieldName != null && SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
    }
}
