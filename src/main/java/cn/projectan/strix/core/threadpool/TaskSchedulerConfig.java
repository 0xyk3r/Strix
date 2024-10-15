package cn.projectan.strix.core.threadpool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author ProjectAn
 * @since 2024-10-14 17:44:44
 */
@Configuration
public class TaskSchedulerConfig {

    @Bean(name = "taskScheduler")
    public ScheduledExecutorService taskScheduler() {
        return Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("task-scheduler");
            return thread;
        });
    }

}
