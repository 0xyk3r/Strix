package cn.projectan.strix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

/**
 * 跨域配置
 *
 * @author ProjectAn
 * @date 2021/05/02 17:11
 */
@Configuration
public class CorsConfig {

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许任何来源
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));
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
