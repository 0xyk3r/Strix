package cn.projectan.strix.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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

}
