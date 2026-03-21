package cn.projectan.strix.core.threadpool;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局可完成异步任务线程池
 *
 * @author ProjectAn
 * @since 2023/9/29 17:55
 */
public class CompletableThreadPool {

    private static final int CORE_POOL_SIZE = 20;
    private static final int MAX_POOL_SIZE = 50;
    private static final int QUEUE_CAPACITY = 1000;
    private static final int KEEP_ALIVE_SECONDS = 300;

    private static volatile ThreadPoolTaskExecutor INSTANCE;

    private CompletableThreadPool() {
    }

    public static ThreadPoolTaskExecutor getInstance() {
        if (INSTANCE == null) {
            synchronized (CompletableThreadPool.class) {
                if (INSTANCE == null) {
                    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                    executor.setCorePoolSize(CORE_POOL_SIZE);
                    executor.setMaxPoolSize(MAX_POOL_SIZE);
                    executor.setQueueCapacity(QUEUE_CAPACITY);
                    executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
                    executor.setThreadNamePrefix("strix-completable-executor-");
                    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    executor.initialize();
                    INSTANCE = executor;
                }
            }
        }
        return INSTANCE;
    }

}
