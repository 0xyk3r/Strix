package cn.projectan.strix.core.delayedtask;

import cn.projectan.strix.model.properties.StrixDelayedTaskProperties;
import cn.projectan.strix.util.ThreadUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 延迟任务管理器
 * 基于 Redis Sorted Set + 定时扫描实现分布式延迟任务
 *
 * @author ProjectAn
 * @since 2024-12-18
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.delayed-task", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DelayedTaskManager {

    private final RedissonClient redissonClient;
    private final StrixDelayedTaskProperties properties;

    /**
     * 虚拟线程执行器，用于并发执行消费者逻辑
     */
    private ExecutorService virtualExecutor;

    /**
     * 扫描器管理
     * Key: 队列名称, Value: 队列扫描器
     */
    private final ConcurrentHashMap<String, QueueScanner> scanners = new ConcurrentHashMap<>();

    /**
     * Lua 脚本：原子获取并删除到期任务
     */
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local now = ARGV[1]
            local limit = ARGV[2]
            
            -- 获取到期任务（score <= now）
            local items = redis.call('ZRANGEBYSCORE', key, 0, now, 'LIMIT', 0, limit)
            
            -- 删除已获取的任务
            if #items > 0 then
                redis.call('ZREM', key, unpack(items))
            end
            
            return items
            """;

    @PostConstruct
    public void init() {
        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Strix DelayedTask: 初始化完成, BatchSize: {}.", properties.getBatchSize());
    }

    /**
     * 添加延迟任务
     *
     * @param queueName 队列名称
     * @param taskId    任务ID（通常是订单ID、工作流任务ID等）
     * @param delay     延迟时间
     * @param timeUnit  时间单位
     */
    public void schedule(String queueName, String taskId, long delay, TimeUnit timeUnit) {
        try {
            long executeTime = System.currentTimeMillis() + timeUnit.toMillis(delay);
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(getRedisKey(queueName));
            sortedSet.add(executeTime, taskId);
            log.debug("Scheduled task [{}] in queue [{}] with delay {}ms", taskId, queueName, timeUnit.toMillis(delay));
        } catch (Exception e) {
            log.error("Failed to schedule task [{}] in queue [{}]", taskId, queueName, e);
            throw new RuntimeException("Failed to schedule delayed task", e);
        }
    }

    /**
     * 取消任务
     *
     * @param queueName 队列名称
     * @param taskId    任务ID
     */
    public void cancel(String queueName, String taskId) {
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(getRedisKey(queueName));
            boolean removed = sortedSet.remove(taskId);
            if (removed) {
                log.debug("Cancelled task [{}] in queue [{}]", taskId, queueName);
            }
        } catch (Exception e) {
            log.error("Failed to cancel task [{}] in queue [{}]", taskId, queueName, e);
        }
    }

    /**
     * 注册消费者
     *
     * @param queueName    队列名称
     * @param consumer     消费者回调函数
     * @param scanInterval 扫描间隔
     * @param timeUnit     时间单位
     */
    public void registerConsumer(String queueName, Consumer<String> consumer, long scanInterval, TimeUnit timeUnit) {
        if (scanners.containsKey(queueName)) {
            log.warn("Consumer for queue [{}] already registered, skipping", queueName);
            return;
        }

        try {
            QueueScanner scanner = new QueueScanner(queueName, consumer, scanInterval, timeUnit);
            scanners.put(queueName, scanner);
            scanner.start();
            log.info("Strix DelayedTask: 已注册消费者 [{}], 扫描间隔: {}s",
                    queueName, timeUnit.toSeconds(scanInterval));
        } catch (Exception e) {
            log.error("Failed to register consumer for queue [{}]", queueName, e);
            throw new RuntimeException("Failed to register consumer", e);
        }
    }

    /**
     * 获取 Redis Key
     *
     * @param queueName 队列名称
     * @return Redis Key
     */
    private String getRedisKey(String queueName) {
        return "strix:delayed-task:" + queueName;
    }

    /**
     * 队列扫描器（内部类）
     */
    private class QueueScanner {
        private final String queueName;
        private final Consumer<String> consumer;
        private final long scanInterval;
        private final TimeUnit timeUnit;
        private final ScheduledExecutorService scheduler;
        private volatile boolean running = true;

        public QueueScanner(String queueName, Consumer<String> consumer, long scanInterval, TimeUnit timeUnit) {
            this.queueName = queueName;
            this.consumer = consumer;
            this.scanInterval = scanInterval;
            this.timeUnit = timeUnit;

            // 创建定时调度器（使用虚拟线程）
            this.scheduler = Executors.newScheduledThreadPool(
                    1,
                    Thread.ofVirtual()
                            .name("delayed-task-scanner-" + queueName + "-", 0)
                            .factory()
            );
        }

        /**
         * 启动扫描器
         */
        public void start() {
            scheduler.scheduleWithFixedDelay(
                    this::scanAndProcess,
                    0,
                    scanInterval,
                    timeUnit
            );
        }

        /**
         * 扫描并处理到期任务
         */
        private void scanAndProcess() {
            if (!running) {
                return;
            }

            try {
                // 使用 Lua 脚本原子获取到期任务
                List<String> expiredTasks = fetchExpiredTasks();

                if (expiredTasks != null && !expiredTasks.isEmpty()) {
                    log.debug("Found {} expired tasks in queue [{}]", expiredTasks.size(), queueName);

                    // 在虚拟线程中并发处理任务
                    for (String taskId : expiredTasks) {
                        virtualExecutor.submit(() -> processTask(taskId));
                    }
                }
            } catch (Exception e) {
                log.error("Error occurred while scanning queue [{}]", queueName, e);
                // 异常不中断扫描器运行
            }
        }

        /**
         * 使用 Lua 脚本原子获取到期任务
         *
         * @return 到期任务ID列表
         */
        private List<String> fetchExpiredTasks() {
            try {
                String key = getRedisKey(queueName);
                long now = System.currentTimeMillis();
                int limit = properties.getBatchSize();

                return redissonClient.getScript(StringCodec.INSTANCE).eval(
                        RScript.Mode.READ_WRITE,
                        LUA_SCRIPT,
                        RScript.ReturnType.MAPVALUELIST,
                        Collections.singletonList(key),
                        String.valueOf(now),
                        String.valueOf(limit)
                );
            } catch (Exception e) {
                log.error("Failed to fetch expired tasks from queue [{}]", queueName, e);
                return Collections.emptyList();
            }
        }

        /**
         * 处理单个任务
         *
         * @param taskId 任务ID
         */
        private void processTask(String taskId) {
            try {
                log.debug("Processing task [{}] from queue [{}]", taskId, queueName);
                consumer.accept(taskId);
            } catch (Exception e) {
                log.error("Error processing task [{}] from queue [{}]", taskId, queueName, e);
                // 消费者异常不影响其他任务处理
            }
        }

        /**
         * 停止扫描器
         */
        public void stop() {
            running = false;
            ThreadUtil.shutdownAndAwaitTermination(scheduler);
            log.info("Strix DelayedTask: 扫描器 [{}] 已停止.", queueName);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Strix DelayedTask: 正在停止中...");

        // 停止所有扫描器
        scanners.values().forEach(QueueScanner::stop);
        scanners.clear();

        // 关闭虚拟线程池
        ThreadUtil.shutdownAndAwaitTermination(virtualExecutor);

        log.info("Strix DelayedTask: 停止完成.");
    }

}
