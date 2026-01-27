package cn.projectan.strix.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TimeZone;

/**
 * Jackson 配置
 *
 * @author ProjectAn
 * @since 2021/5/13 19:19
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式常量
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String TIME_ZONE = "GMT+8";

    /**
     * 需要排除的字段
     */
    private final Set<String> excludeFields = Set.of(
            "password",
            "newPassword",
            "oldPassword",
            "confirmPassword",
            "loginPassword"
    );

    /**
     * 创建配置好的 JavaTimeModule
     * <p>统一配置 LocalDateTime, LocalDate, LocalTime 的序列化和反序列化
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        // LocalDateTime
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));

        // LocalDate
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

        // LocalTime
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        return module;
    }

    /**
     * Jackson 配置
     *
     * @param builder        Jackson2ObjectMapperBuilder
     * @param javaTimeModule 时间模块
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder, JavaTimeModule javaTimeModule) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));

        // 过滤字段
        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept(excludeFields);
        FilterProvider filterProvider = new SimpleFilterProvider().addFilter("strixFilter", filter);
        objectMapper.setFilterProvider(filterProvider);

        // 注册时间模块
        objectMapper.registerModule(javaTimeModule);

        // 用于忽略使用 @JsonSubTypes 时该参数不存在报错的情况
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);

        return objectMapper;
    }

    /**
     * Jackson 自定义配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(JavaTimeModule javaTimeModule) {
        return builder -> builder
                .timeZone(TIME_ZONE)
                .modules(javaTimeModule);
    }

}
