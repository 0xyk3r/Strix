package cn.projectan.strix.config;

import cn.projectan.strix.core.datamask.DataMaskAnnotationIntrospector;
import cn.projectan.strix.core.xss.XssStringDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import java.text.SimpleDateFormat;
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
     * Spring Boot 4 Jackson 3 配置定制器
     * <p>此方法用于定制 Spring Boot 自动配置的 ObjectMapper，
     * 使其应用 DataMask 数据脱敏和自定义日期时间格式
     *
     * @return JsonMapperBuilderCustomizer
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            // 字段过滤
            SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept(excludeFields);
            FilterProvider filterProvider = new SimpleFilterProvider().addFilter("strixFilter", filter);

            builder
                    // 日期时间格式
                    .defaultDateFormat(new SimpleDateFormat(DATE_TIME_PATTERN))
                    .defaultTimeZone(TimeZone.getDefault())
                    // 自定义模块 (LocalDateTime, LocalDate, LocalTime)
                    .addModule(javaTimeModule())
                    .addModule(xssFilterModule())
                    // 属性包含规则
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                    .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
                    // 反序列化配置
                    .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
                    // 过滤器
                    .filterProvider(filterProvider)
                    // Strix DataMask 数据脱敏工具 (合并默认的注解解析器以支持 @JsonProperty 等)
                    .annotationIntrospector(AnnotationIntrospectorPair.pair(
                            new DataMaskAnnotationIntrospector(),
                            new JacksonAnnotationIntrospector()
                    ));
        };
    }

    /**
     * 创建配置好的 JavaTimeModule
     * <p>统一配置 LocalDateTime, LocalDate, LocalTime 的序列化和反序列化
     */
    public static JacksonModule javaTimeModule() {
        SimpleModule module = new SimpleModule();

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
     * 创建 XSS 过滤器模块
     */
    public static JacksonModule xssFilterModule() {
        SimpleModule module = new SimpleModule("XssProtectionModule");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }

    /**
     * 创建全局基础的 JsonMapper.Builder
     *
     * @return JsonMapper.Builder
     */
    public static JsonMapper.Builder builder() {
        return JsonMapper.builder()
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .defaultTimeZone(TimeZone.getDefault())
                .addModule(javaTimeModule())
                .addModule(xssFilterModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
                .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);
    }

}
