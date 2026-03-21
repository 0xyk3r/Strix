package cn.projectan.strix.config;

import cn.projectan.strix.core.ratelimit.RateLimitInterceptor;
import cn.projectan.strix.core.xss.XssFilter;
import cn.projectan.strix.model.properties.system.StrixRateLimitProperties;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.context.ContextInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.Executor;

/**
 * WebConfig
 *
 * @author ProjectAn
 * @since 2023/12/9 16:24
 */
@Configuration
@EnableConfigurationProperties(StrixRateLimitProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final Executor mvcAsyncExecutor;
    private final RedisUtil redisUtil;
    private final StrixRateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public WebConfig(@Qualifier("mvcAsyncExecutor") Executor mvcAsyncExecutor,
                     RedisUtil redisUtil,
                     StrixRateLimitProperties rateLimitProperties,
                     ObjectMapper objectMapper) {
        this.mvcAsyncExecutor = mvcAsyncExecutor;
        this.redisUtil = redisUtil;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * MVC 异步支持
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(300_000L);
        configurer.setTaskExecutor(new TaskExecutorAdapter(mvcAsyncExecutor));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(redisUtil, rateLimitProperties, objectMapper));
        registry.addInterceptor(new ContextInterceptor());
    }

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

}
