package cn.projectan.strix.util.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式同步操作工具类
 * <p>基于 Redisson 实现分布式锁，提供以下功能：
 * <ul>
 *     <li>普通互斥锁：{@link #exec(String, Runnable)}</li>
 *     <li>带返回值的锁：{@link #execWithResult(String, Supplier)}</li>
 *     <li>读写锁：{@link #execRead(String, Supplier)} / {@link #execWrite(String, Runnable)}</li>
 *     <li>非阻塞尝试锁：{@link #tryExec(String, Runnable)}</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2022/4/4 19:24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronizedUtil {

    /**
     * 锁 Key 前缀
     */
    private static final String LOCK_PREFIX = "strix:lock:";

    /**
     * 读写锁 Key 前缀
     */
    private static final String RW_LOCK_PREFIX = "strix:lock:rw:";

    /**
     * 默认等待时间（秒）
     */
    private static final int DEFAULT_WAIT_TIME = 10;

    /**
     * 默认锁过期时间（秒）
     */
    private static final int DEFAULT_LEASE_TIME = 30;

    private final RedissonClient redissonClient;

    // ==================== 普通互斥锁 ====================

    /**
     * 执行同步操作
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param runnable 操作
     */
    public void exec(String key, Runnable runnable) {
        exec(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, runnable);
    }

    /**
     * 执行同步操作
     * <p>默认占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param waitTime 等待时间(s)
     * @param runnable 操作
     */
    public void exec(String key, int waitTime, Runnable runnable) {
        exec(key, waitTime, DEFAULT_LEASE_TIME, runnable);
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
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        boolean isLock;
        try {
            // 最多等待 {waitTime} 秒获取锁, 占用锁后, {leaseTime} 秒后自动释放
            isLock = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    runnable.run();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("获取锁: {} 超时, 自动放弃", key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁: {} 失败", key, e);
        }
    }

    // ==================== 带返回值的锁 ====================

    /**
     * 执行同步操作并返回结果
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param supplier 操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execWithResult(String key, Supplier<T> supplier) {
        return execWithResult(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, supplier);
    }

    /**
     * 执行同步操作并返回结果
     *
     * @param key       锁的key
     * @param waitTime  等待时间(s)
     * @param leaseTime 锁的过期时间(s)
     * @param supplier  操作
     * @param <T>       返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execWithResult(String key, int waitTime, int leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        boolean isLock;
        try {
            isLock = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("获取锁: {} 超时, 自动放弃", key);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁: {} 失败", key, e);
            return null;
        }
    }

    // ==================== 读写锁 ====================

    /**
     * 执行读操作（共享锁）
     * <p>多个线程可以同时获取读锁，但读写互斥。
     * <p>适用于读多写少的场景，可以提高并发读取性能。
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param supplier 操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execRead(String key, Supplier<T> supplier) {
        return execRead(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, supplier);
    }

    /**
     * 执行读操作（共享锁）
     * <p>多个线程可以同时获取读锁，但读写互斥。
     * <p>适用于读多写少的场景，可以提高并发读取性能。
     *
     * @param key       锁的key
     * @param waitTime  等待时间(s)
     * @param leaseTime 锁的过期时间(s)
     * @param supplier  操作
     * @param <T>       返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execRead(String key, int waitTime, int leaseTime, Supplier<T> supplier) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(RW_LOCK_PREFIX + key);
        RLock readLock = rwLock.readLock();
        boolean isLock;
        try {
            isLock = readLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    return supplier.get();
                } finally {
                    if (readLock.isHeldByCurrentThread()) {
                        readLock.unlock();
                    }
                }
            } else {
                log.warn("获取读锁: {} 超时, 自动放弃", key);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取读锁: {} 失败", key, e);
            return null;
        }
    }

    /**
     * 执行写操作（排他锁）
     * <p>写锁是排他的，同一时刻只有一个线程可以获取写锁，且与读锁互斥。
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param runnable 操作
     */
    public void execWrite(String key, Runnable runnable) {
        execWrite(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, runnable);
    }

    /**
     * 执行写操作（排他锁）
     * <p>写锁是排他的，同一时刻只有一个线程可以获取写锁，且与读锁互斥。
     *
     * @param key       锁的key
     * @param waitTime  等待时间(s)
     * @param leaseTime 锁的过期时间(s)
     * @param runnable  操作
     */
    public void execWrite(String key, int waitTime, int leaseTime, Runnable runnable) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(RW_LOCK_PREFIX + key);
        RLock writeLock = rwLock.writeLock();
        boolean isLock;
        try {
            isLock = writeLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    runnable.run();
                } finally {
                    if (writeLock.isHeldByCurrentThread()) {
                        writeLock.unlock();
                    }
                }
            } else {
                log.warn("获取写锁: {} 超时, 自动放弃", key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取写锁: {} 失败", key, e);
        }
    }

    /**
     * 执行写操作并返回结果（排他锁）
     * <p>写锁是排他的，同一时刻只有一个线程可以获取写锁，且与读锁互斥。
     * <p>默认等待10s, 占用30s后自动解锁</p>
     *
     * @param key      锁的key
     * @param supplier 操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execWriteWithResult(String key, Supplier<T> supplier) {
        return execWriteWithResult(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, supplier);
    }

    /**
     * 执行写操作并返回结果（排他锁）
     * <p>写锁是排他的，同一时刻只有一个线程可以获取写锁，且与读锁互斥。
     *
     * @param key       锁的key
     * @param waitTime  等待时间(s)
     * @param leaseTime 锁的过期时间(s)
     * @param supplier  操作
     * @param <T>       返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T execWriteWithResult(String key, int waitTime, int leaseTime, Supplier<T> supplier) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(RW_LOCK_PREFIX + key);
        RLock writeLock = rwLock.writeLock();
        boolean isLock;
        try {
            isLock = writeLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    return supplier.get();
                } finally {
                    if (writeLock.isHeldByCurrentThread()) {
                        writeLock.unlock();
                    }
                }
            } else {
                log.warn("获取写锁: {} 超时, 自动放弃", key);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取写锁: {} 失败", key, e);
            return null;
        }
    }

    // ==================== 非阻塞尝试锁 ====================

    /**
     * 尝试执行同步操作（非阻塞）
     * <p>如果无法立即获取锁，则直接返回 false，不会等待。
     * <p>获取锁后，默认占用30s自动解锁</p>
     *
     * @param key      锁的key
     * @param runnable 操作
     * @return 是否成功执行（true=成功获取锁并执行，false=获取锁失败）
     */
    public boolean tryExec(String key, Runnable runnable) {
        return tryExec(key, DEFAULT_LEASE_TIME, runnable);
    }

    /**
     * 尝试执行同步操作（非阻塞）
     * <p>如果无法立即获取锁，则直接返回 false，不会等待。
     *
     * @param key       锁的key
     * @param leaseTime 锁的过期时间(s)
     * @param runnable  操作
     * @return 是否成功执行（true=成功获取锁并执行，false=获取锁失败）
     */
    public boolean tryExec(String key, int leaseTime, Runnable runnable) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        boolean isLock;
        try {
            // waitTime=0 表示不等待，获取不到立即返回
            isLock = lock.tryLock(0, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    runnable.run();
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.debug("获取锁: {} 失败, 跳过执行", key);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("尝试获取锁: {} 失败", key, e);
            return false;
        }
    }

    /**
     * 尝试执行同步操作并返回结果（非阻塞）
     * <p>如果无法立即获取锁，则直接返回 null，不会等待。
     * <p>获取锁后，默认占用30s自动解锁</p>
     *
     * @param key      锁的key
     * @param supplier 操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T tryExecWithResult(String key, Supplier<T> supplier) {
        return tryExecWithResult(key, DEFAULT_LEASE_TIME, supplier);
    }

    /**
     * 尝试执行同步操作并返回结果（非阻塞）
     * <p>如果无法立即获取锁，则直接返回 null，不会等待。
     *
     * @param key       锁的key
     * @param leaseTime 锁的过期时间(s)
     * @param supplier  操作
     * @param <T>       返回值类型
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T tryExecWithResult(String key, int leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        boolean isLock;
        try {
            isLock = lock.tryLock(0, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.debug("获取锁: {} 失败, 跳过执行", key);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("尝试获取锁: {} 失败", key, e);
            return null;
        }
    }

}
