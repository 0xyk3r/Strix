package cn.projectan.strix.core.module.oauth;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Strix OAuth 线程池配置
 *
 * @author ProjectAn
 * @since 2024-10-14 16:49:22
 */
@Slf4j
@Configuration
public class StrixOAuthSchedulerConfig {

    private ScheduledExecutorService scheduler;

    /**
     * Strix OAuth 线程池
     *
     * @return Strix OAuth 线程池
     */
    @Bean(name = "strixOAuthScheduler")
    public ScheduledExecutorService strixOAuthScheduler() {
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("strix-oauth-scheduler");
            return thread;
        });
        return scheduler;
    }

    /**
     * 关闭 Strix OAuth 线程池
     */
    @PreDestroy
    public void shutdownStrixOAuthScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
