package cn.projectan.strix.config;

import cn.projectan.strix.model.properties.system.StrixCorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 *
 * @author ProjectAn
 * @since 2021/05/02 17:11
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StrixCorsProperties.class)
public class CorsConfig {

    private final StrixCorsProperties corsProperties;

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 从配置文件读取允许的域名列表
        corsConfiguration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        // 允许任何请求头
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // 允许任何方法
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        // 允许凭证
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }

}
