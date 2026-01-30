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
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public GroupedOpenApi strixSystemApi() {
        return GroupedOpenApi.builder()
                .group("Strix 管理中心接口")
                .pathsToMatch(
                        "/system/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi strixSrvApi() {
        return GroupedOpenApi.builder()
                .group("Strix 通用服务接口")
                .pathsToMatch(
                        "/srv/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi strixPayApi() {
        return GroupedOpenApi.builder()
                .group("Strix 通用支付接口")
                .pathsToMatch(
                        "/pay/**"
                )
                .build();
    }

    /**
     * 业务接口（默认主要展示）
     */
    @Bean
    public GroupedOpenApi bizApi() {
        return GroupedOpenApi.builder()
                .group("业务接口")
                .pathsToMatch("/api/**")
                .pathsToExclude(
                        "/system/**",
                        "/srv/**",
                        "/pay/**"
                )
                .build();
    }

//    @Bean
//    public OpenAPI openAPI() {
//        return new OpenAPI()
//                .components(new Components()
//                        .addSecuritySchemes("tokenHeader",
//                                new SecurityScheme()
//                                        .name("token")
//                                        .type(SecurityScheme.Type.APIKEY)
//                                        .in(SecurityScheme.In.HEADER)
//                                        .description("自定义 Token Header")
//                        )
//                        .addSecuritySchemes("ssPwdHeader",
//                                new SecurityScheme()
//                                        .name("ss-pwd")
//                                        .type(SecurityScheme.Type.APIKEY)
//                                        .in(SecurityScheme.In.HEADER)
//                                        .description("请求密码 Header")
//                        )
//                )
//                // 👇 全局生效
//                .addSecurityItem(new SecurityRequirement()
//                        .addList("tokenHeader")
//                        .addList("ssPwdHeader")
//                );
//    }

}
