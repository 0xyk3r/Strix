package cn.projectan.strix.core.threadpool;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ProjectAn
 * @since 2023/9/29 17:55
 */
public class CompletableThreadPool {

    private static ThreadPoolTaskExecutor INSTANCE;

    private CompletableThreadPool() {
    }

    public static ThreadPoolTaskExecutor getInstance() {
        if (INSTANCE == null) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(20);
            executor.setMaxPoolSize(50);
            executor.setQueueCapacity(1000);
            executor.setKeepAliveSeconds(300);
            executor.setThreadNamePrefix("strix-completable-executor-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.initialize();
            INSTANCE = executor;
        }
        return INSTANCE;
    }

}
