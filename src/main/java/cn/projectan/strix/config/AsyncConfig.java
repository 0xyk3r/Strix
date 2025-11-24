package cn.projectan.strix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务配置
 *
 * @author ProjectAn
 * @since 2025/4/10 10:33
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 通用异步任务执行器
     * 用于 @Async 注解的方法
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(executorService);
    }

    /**
     * 通用异步 MVC 任务执行器
     * 用于 MVC 异步请求处理（Callable/DeferredResult 等）
     */
    @Bean(name = "mvnAsyncExecutor")
    public Executor mvnAsyncExecutor() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(executorService);
    }

}
