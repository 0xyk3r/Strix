package cn.projectan.strix.service.impl;

import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.service.AsyncSystemLogService;
import cn.projectan.strix.service.SystemLogService;
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
 * 异步系统日志服务实现
 * <p>
 * 特性：
 * 1. 使用阻塞队列缓冲日志，避免直接写数据库
 * 2. 定时批量插入，提高数据库写入效率
 * 3. 应用关闭时自动刷新剩余日志
 * </p>
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.log", name = "enable", havingValue = "true")
public class AsyncSystemLogServiceImpl implements AsyncSystemLogService {

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
    @Override
    public CompletableFuture<Void> saveAsync(SystemLog systemLog) {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean offered = logQueue.offer(systemLog);
                if (!offered) {
                    log.warn("System log queue is full, log may be lost: {}", systemLog.getOperationName());
                    // 队列满时直接保存到数据库
                    systemLogService.save(systemLog);
                }
            } catch (Exception e) {
                log.error("Failed to add log to queue: {}", e.getMessage(), e);
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
                log.debug("Batch saved {} system logs", logs.size());
            }
        } catch (Exception e) {
            log.error("Failed to batch save system logs: {}", e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时刷新所有日志
     */
    @PreDestroy
    @Override
    public void flushAll() {
        log.info("Flushing remaining system logs before shutdown...");
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
                log.info("Flushed {} system logs successfully", total);
            }
        } catch (Exception e) {
            log.error("Failed to flush system logs: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前队列大小（用于监控）
     */
    public int getQueueSize() {
        return logQueue.size();
    }
}
