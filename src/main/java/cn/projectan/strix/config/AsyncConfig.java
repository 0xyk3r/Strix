package cn.projectan.strix.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务配置
 *
 * @author ProjectAn
 * @since 2025/4/10 10:33
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private final List<ExecutorService> executorServices = new ArrayList<>();

    /**
     * 通用异步任务执行器
     * 用于 @Async 注解的方法
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        executorServices.add(executorService);
        return new TaskExecutorAdapter(executorService);
    }

    /**
     * 通用异步 MVC 任务执行器
     * 用于 MVC 异步请求处理（Callable/DeferredResult 等）
     */
    @Bean(name = "mvcAsyncExecutor")
    public Executor mvcAsyncExecutor() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        executorServices.add(executorService);
        return new TaskExecutorAdapter(executorService);
    }

    @PreDestroy
    public void shutdown() {
        executorServices.forEach(ExecutorService::close);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Async method {}.{}() execution failed: {}",
                        method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage(), ex);
    }

}
