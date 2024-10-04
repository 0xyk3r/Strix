package cn.projectan.strix.core.threadpool;

import cn.projectan.strix.utils.ThreadUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ProjectAn
 * @since 2022/10/2 19:51
 */
@Configuration
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
        return new ScheduledThreadPoolExecutor(50,
                new BasicThreadFactory.Builder().namingPattern("strix-schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                ThreadUtil.printException(r, t);
            }
        };
    }

}
