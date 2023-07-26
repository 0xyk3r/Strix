package cn.projectan.strix.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * @author 安炯奕
 * @date 2021/5/13 19:19
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept("password", "newPassword", "oldPassword", "confirmPassword", "loginPassword");
        FilterProvider filterProvider = new SimpleFilterProvider().addFilter("passwordFilter", filter);
        objectMapper.setFilterProvider(filterProvider);

        return objectMapper;
    }

}
