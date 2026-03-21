package cn.projectan.strix.core.threadpool;

import cn.projectan.strix.util.system.ThreadUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.*;

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
        // 日志保存为 I/O 密集型操作（数据库写入），虚拟线程可高效处理大量并发写入且无需调优线程池参数
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(executorService);
    }

    @Bean(name = "strixScheduledExecutor")
    public ScheduledExecutorService strixScheduledExecutor() {
        // 调度线程仅负责触发定时任务，2 个线程足够；实际执行可委托给虚拟线程
        return new ScheduledThreadPoolExecutor(2,
                Thread.ofPlatform().daemon(true).name("strix-schedule-pool-", 0).factory(),
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                ThreadUtil.printException(r, t);
            }
        };
    }

}
