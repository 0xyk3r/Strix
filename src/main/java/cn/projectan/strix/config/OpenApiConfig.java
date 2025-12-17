package cn.projectan.strix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableKnife4j
@OpenAPIDefinition(
        info = @Info(
                title = "Strix API 文档",
                version = "1.0.0",
                description = "Strix API 文档",
                contact = @Contact(
                        name = "ProjectAn"
                )
        )
)
public class OpenApiConfig {

    private final ObjectMapper objectMapper;

    public OpenApiConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void customizeSwaggerModelResolver() {
        TypeNameResolver customResolver = new TypeNameResolver() {
            @Override
            protected String getNameOfClass(Class<?> cls) {
                // 使用类的全限定名，避免同名冲突
                return cls.getName().replace("cn.projectan.strix.", "");
            }
        };

        // 将自定义的 TypeNameResolver 注入到 ModelResolver 中
        ModelConverters.getInstance().addConverter(new ModelResolver(objectMapper, customResolver));
    }

}
