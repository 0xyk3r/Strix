package cn.projectan.strix.core.threadpool;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ProjectAn
 * @since 2024-10-14 17:44:44
 */
@Slf4j
@Configuration
public class TaskSchedulerConfig {

    private ScheduledExecutorService taskScheduler;

    @Bean(name = "taskScheduler")
    public ScheduledExecutorService taskScheduler() {
        taskScheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("task-scheduler-", 0).factory());
        return taskScheduler;
    }

    @PreDestroy
    public void shutdown() {
        if (taskScheduler != null && !taskScheduler.isShutdown()) {
            taskScheduler.shutdown();
            try {
                if (!taskScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    taskScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("TaskScheduler 已关闭");
        }
    }

}
