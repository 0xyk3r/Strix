package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.SystemLog;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 异步系统日志服务
 * <p>
 * <p>特性：
 * <p>1. 使用阻塞队列缓冲日志，避免直接写数据库
 * <p>2. 定时批量插入，提高数据库写入效率
 * <p>3. 应用关闭时自动刷新剩余日志
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.log", name = "enable", havingValue = "true")
public class AsyncSystemLogService {

    /**
     * 日志队列容量
     */
    private static final int QUEUE_CAPACITY = 10000;

    /**
     * 批量插入的批次大小
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 日志队列
     */
    private final BlockingQueue<SystemLog> logQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final SystemLogService systemLogService;

    /**
     * 异步保存日志
     */
    @Async("strixThreadExecutor")
    public CompletableFuture<Void> saveAsync(SystemLog systemLog) {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean offered = logQueue.offer(systemLog);
                if (!offered) {
                    log.warn("Strix Log: 系统日志队列已满，日志可能会丢失.");
                    // 队列满时直接保存到数据库 可能产生性能问题
                    systemLogService.save(systemLog);
                }
            } catch (Exception e) {
                log.error("Strix Log: 添加日志到队列失败, 错误: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 定时批量保存日志
     * 每5秒执行一次，或队列达到批次大小时触发
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void batchSaveLogs() {
        try {
            List<SystemLog> logs = new ArrayList<>(BATCH_SIZE);
            logQueue.drainTo(logs, BATCH_SIZE);

            if (!logs.isEmpty()) {
                systemLogService.saveBatch(logs);
                log.debug("Strix Log: 成功批量保存 {} 条系统日志.", logs.size());
            }
        } catch (Exception e) {
            log.error("Strix Log: 批量保存系统日志失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时刷新所有日志
     */
    @PreDestroy
    public void flushAll() {
        log.info("Strix Log: 应用关闭, 处理队列中剩余系统日志中...");
        try {
            List<SystemLog> remainingLogs = new ArrayList<>();
            logQueue.drainTo(remainingLogs);

            if (!remainingLogs.isEmpty()) {
                // 分批保存
                int total = remainingLogs.size();
                for (int i = 0; i < total; i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, total);
                    List<SystemLog> batch = remainingLogs.subList(i, end);
                    systemLogService.saveBatch(batch);
                }
                log.info("Strix Log: 成功处理队列中剩余的 {} 条系统日志.", total);
            }
        } catch (Exception e) {
            log.error("Strix Log: 批量保存系统日志失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前队列大小（用于监控）
     */
    public int getQueueSize() {
        return logQueue.size();
    }

}
