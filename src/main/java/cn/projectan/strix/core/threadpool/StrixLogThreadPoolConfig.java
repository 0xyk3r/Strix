package cn.projectan.strix.core.threadpool;

import cn.projectan.strix.util.ThreadUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strix 日志线程池配置
 * <p>
 * 配置说明：
 * 1. strixThreadExecutor: 用于异步日志保存的线程池
 * 2. strixScheduledExecutor: 用于定时批量写入日志的调度线程池
 * </p>
 *
 * @author ProjectAn
 * @since 2022/10/2 19:51
 */
@Configuration
@EnableAsync
@EnableScheduling
public class StrixLogThreadPoolConfig {

    @Bean(name = "strixThreadExecutor")
    public Executor strixThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("strix-log-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean(name = "strixScheduledExecutor")
    public Executor strixScheduledExecutor() {
        return new ScheduledThreadPoolExecutor(50, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("strix-schedule-pool-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        }, new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                ThreadUtil.printException(r, t);
            }
        };
    }

}
