package cn.projectan.strix.config;

import cn.projectan.strix.util.context.ContextInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * WebConfig
 *
 * @author ProjectAn
 * @since 2023/12/9 16:24
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Executor mvnAsyncExecutor;

    public WebConfig(@Qualifier("mvnAsyncExecutor") Executor mvnAsyncExecutor) {
        this.mvnAsyncExecutor = mvnAsyncExecutor;
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
        configurer.setTaskExecutor(new TaskExecutorAdapter(mvnAsyncExecutor));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ContextInterceptor());
    }

}
