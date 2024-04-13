package cn.projectan.strix.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 同步操作工具类
 *
 * @author ProjectAn
 * @date 2022/4/4 19:24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronizedUtil {

    private final RedissonClient redissonClient;

    /**
     * 执行同步操作
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param runnable 操作
     */
    public void exec(String key, Runnable runnable) {
        exec(key, 10, 30, runnable);
    }

    /**
     * 执行同步操作
     * <p>默认占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param waitTime 等待时间
     * @param runnable 操作
     */
    public void exec(String key, int waitTime, Runnable runnable) {
        exec(key, waitTime, 30, runnable);
    }

    /**
     * 执行同步操作
     *
     * @param key       锁的key
     * @param waitTime  等待时间(s)
     * @param leaseTime 锁的过期时间(s)
     * @param runnable  操作
     */
    public void exec(String key, int waitTime, int leaseTime, Runnable runnable) {
        RLock lock = redissonClient.getLock("strix:lock:" + key);
        boolean isLock;
        try {
            // 最多等待 {waitTime} 秒获取锁, 占用锁后, {leaseTime} 秒后自动释放
            isLock = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    runnable.run();
                } finally {
                    if (lock.isLocked()) {
                        lock.unlock();
                    }
                }
            } else {
                log.error("获取锁: {} 超时, 自动放弃", key);
            }
        } catch (InterruptedException e) {
            log.error("获取锁: {} 失败", key, e);
        }
    }

}
