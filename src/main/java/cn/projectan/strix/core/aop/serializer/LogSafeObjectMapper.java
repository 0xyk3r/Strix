package cn.projectan.strix.core.aop.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 日志安全的 ObjectMapper - 用于序列化日志参数
 * <p>
 * 特性：
 * 1. 自动过滤不可序列化的对象（ServletRequest、ServletResponse、MultipartFile等）
 * 2. 自动脱敏密码相关字段
 * 3. 防止循环引用
 * 4. 优雅处理序列化异常
 * </p>
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
@Slf4j
public class LogSafeObjectMapper extends ObjectMapper {

    /**
     * 不可序列化的类型
     */
    private static final Set<Class<?>> UNSERIALIZABLE_TYPES = Set.of(
            ServletRequest.class,
            ServletResponse.class,
            MultipartFile.class
    );

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

    private static final String MASKED_VALUE = "******";
    private static final String FILTERED_VALUE = "[FILTERED]";

    public LogSafeObjectMapper() {
        super();
        configure();
    }

    private void configure() {
        // 基本配置
        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.registerModule(new JavaTimeModule());

        // 注册自定义模块
        SimpleModule module = new SimpleModule();

        // 为不可序列化类型注册过滤序列化器
        module.addSerializer(ServletRequest.class, new FilteredSerializer());
        module.addSerializer(ServletResponse.class, new FilteredSerializer());
        module.addSerializer(MultipartFile.class, new FilteredSerializer());

        // 添加 Bean 序列化修改器来处理密码字段
        module.setSerializerModifier(new SensitiveDataSerializerModifier());

        this.registerModule(module);
    }

    /**
     * 安全序列化对象数组为JSON字符串
     */
    public String safeWriteValueAsString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            // 过滤掉不可序列化的参数
            List<Object> safeArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null) {
                    safeArgs.add(null);
                } else if (isUnserializableType(arg)) {
                    safeArgs.add(FILTERED_VALUE + ":" + arg.getClass().getSimpleName());
                } else {
                    safeArgs.add(arg);
                }
            }

            return this.writeValueAsString(safeArgs);
        } catch (Exception e) {
            log.warn("Failed to serialize log parameters: {}", e.getMessage());
            return "[Serialization Error: " + e.getMessage() + "]";
        }
    }

    /**
     * 安全序列化单个对象为JSON字符串
     */
    public String safeWriteValueAsString(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            if (isUnserializableType(obj)) {
                return FILTERED_VALUE + ":" + obj.getClass().getSimpleName();
            }
            return this.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize log object: {}", e.getMessage());
            return "[Serialization Error: " + e.getMessage() + "]";
        }
    }

    /**
     * 判断对象是否为不可序列化类型
     */
    private boolean isUnserializableType(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> objClass = obj.getClass();
        for (Class<?> type : UNSERIALIZABLE_TYPES) {
            if (type.isAssignableFrom(objClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤序列化器 - 用于不可序列化的类型
     */
    private static class FilteredSerializer extends StdSerializer<Object> {
        protected FilteredSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(FILTERED_VALUE + ":" + value.getClass().getSimpleName());
        }
    }

    /**
     * Bean 序列化修改器 - 用于处理敏感字段
     */
    private static class SensitiveDataSerializerModifier extends BeanSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(
                com.fasterxml.jackson.databind.SerializationConfig config,
                com.fasterxml.jackson.databind.BeanDescription beanDesc,
                List<BeanPropertyWriter> beanProperties) {

            List<BeanPropertyWriter> newWriters = new ArrayList<>();
            for (BeanPropertyWriter writer : beanProperties) {
                if (isSensitiveField(writer.getName())) {
                    newWriters.add(new SensitiveFieldWriter(writer));
                } else {
                    newWriters.add(writer);
                }
            }
            return newWriters;
        }

        private boolean isSensitiveField(String fieldName) {
            return fieldName != null && SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
        }
    }

    /**
     * 敏感字段写入器 - 将敏感字段值替换为掩码
     */
    private static class SensitiveFieldWriter extends BeanPropertyWriter {
        protected SensitiveFieldWriter(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            gen.writeStringField(this.getName(), MASKED_VALUE);
        }
    }
}
