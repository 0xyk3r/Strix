package cn.projectan.strix.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 延迟队列工具类
 *
 * @author ProjectAn
 * @since 2024/4/14 上午12:33
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayedQueueUtil {

    private final RedissonClient redissonClient;

    /**
     * 添加元素到延迟队列
     *
     * @param queueName 队列名称
     * @param t         元素
     * @param delay     延迟时间
     * @param timeUnit  时间单位
     * @param <T>       泛型
     */
    public <T> void offer(String queueName, T t, long delay, TimeUnit timeUnit) {
        RBlockingDeque<T> blockingDeque = redissonClient.getBlockingDeque(queueName);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        delayedQueue.offer(t, delay, timeUnit);
    }

    /**
     * 获取延迟队列
     *
     * @param queueName 队列名称
     * @param <T>       泛型
     * @return 延迟队列
     */
    public <T> RBlockingDeque<T> getQueue(String queueName) {
        RBlockingDeque<T> blockingDeque = redissonClient.getBlockingDeque(queueName);
        redissonClient.getDelayedQueue(blockingDeque);
        return blockingDeque;
    }

    /**
     * 移除元素
     *
     * @param queueName 队列名称
     * @param t         元素
     * @param <T>       泛型
     */
    public <T> void remove(String queueName, T t) {
        RBlockingDeque<T> blockingDeque = redissonClient.getBlockingDeque(queueName);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        delayedQueue.remove(t);
    }

}
