package cn.projectan.strix.util.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式限流工具类
 * <p>基于 Redisson 的 {@link RRateLimiter} 实现，使用令牌桶算法进行限流。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // API 接口限流: 每分钟最多 100 次请求
 * if (!rateLimiterUtil.tryAcquire("api:user:list", 100, 1, RateIntervalUnit.MINUTES)) {
 *     throw new BusinessException("请求过于频繁，请稍后再试");
 * }
 *
 * // 短信发送限流: 每分钟最多 1 条
 * if (!rateLimiterUtil.tryAcquire("sms:" + phone, 1, 1, RateIntervalUnit.MINUTES)) {
 *     throw new BusinessException("发送过于频繁");
 * }
 *
 * // IP 限流: 每分钟最多 60 次
 * if (!rateLimiterUtil.tryAcquire("ip:" + ip, 60, 1, RateIntervalUnit.MINUTES)) {
 *     throw new BusinessException("请求过于频繁");
 * }
 * }</pre>
 *
 * @author ProjectAn
 * @since 2024/01/01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterUtil {

    /**
     * 限流器 Key 前缀
     */
    private static final String RATE_LIMITER_PREFIX = "strix:rate-limiter:";

    private final RedissonClient redissonClient;

    /**
     * 尝试获取许可（非阻塞）
     * <p>如果当前没有可用的许可，立即返回 false，不会等待。
     *
     * @param key      限流器标识
     * @param rate     时间间隔内允许的请求数
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return 是否获取成功（true=允许通过，false=被限流）
     */
    public boolean tryAcquire(String key, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        return rateLimiter.tryAcquire();
    }

    /**
     * 尝试获取多个许可（非阻塞）
     * <p>如果当前没有足够的许可，立即返回 false，不会等待。
     *
     * @param key      限流器标识
     * @param permits  需要获取的许可数量
     * @param rate     时间间隔内允许的请求数
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return 是否获取成功（true=允许通过，false=被限流）
     */
    public boolean tryAcquire(String key, int permits, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 尝试获取许可（带超时）
     * <p>如果当前没有可用的许可，最多等待指定时间。
     *
     * @param key         限流器标识
     * @param rate        时间间隔内允许的请求数
     * @param interval    时间间隔
     * @param unit        时间单位
     * @param timeout     最大等待时间
     * @param timeoutUnit 等待时间单位
     * @return 是否获取成功（true=允许通过，false=超时后仍被限流）
     */
    public boolean tryAcquire(String key, long rate, long interval, RateIntervalUnit unit,
                              long timeout, TimeUnit timeoutUnit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        return rateLimiter.tryAcquire(timeout, timeoutUnit);
    }

    /**
     * 尝试获取多个许可（带超时）
     * <p>如果当前没有足够的许可，最多等待指定时间。
     *
     * @param key         限流器标识
     * @param permits     需要获取的许可数量
     * @param rate        时间间隔内允许的请求数
     * @param interval    时间间隔
     * @param unit        时间单位
     * @param timeout     最大等待时间
     * @param timeoutUnit 等待时间单位
     * @return 是否获取成功（true=允许通过，false=超时后仍被限流）
     */
    public boolean tryAcquire(String key, int permits, long rate, long interval, RateIntervalUnit unit,
                              long timeout, TimeUnit timeoutUnit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        return rateLimiter.tryAcquire(permits, timeout, timeoutUnit);
    }

    /**
     * 获取许可（阻塞）
     * <p>如果当前没有可用的许可，将一直阻塞直到获取成功。
     * <p><b>注意：</b>谨慎使用此方法，可能导致线程长时间阻塞。
     *
     * @param key      限流器标识
     * @param rate     时间间隔内允许的请求数
     * @param interval 时间间隔
     * @param unit     时间单位
     */
    public void acquire(String key, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        rateLimiter.acquire();
    }

    /**
     * 获取多个许可（阻塞）
     * <p>如果当前没有足够的许可，将一直阻塞直到获取成功。
     * <p><b>注意：</b>谨慎使用此方法，可能导致线程长时间阻塞。
     *
     * @param key      限流器标识
     * @param permits  需要获取的许可数量
     * @param rate     时间间隔内允许的请求数
     * @param interval 时间间隔
     * @param unit     时间单位
     */
    public void acquire(String key, int permits, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, unit);
        rateLimiter.acquire(permits);
    }

    /**
     * 删除限流器
     * <p>删除后，下次调用会重新创建限流器，计数器会重置。
     *
     * @param key 限流器标识
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_PREFIX + key);
        return rateLimiter.delete();
    }

    /**
     * 获取剩余可用许可数
     *
     * @param key 限流器标识
     * @return 剩余许可数，如果限流器不存在返回 0
     */
    public long availablePermits(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_PREFIX + key);
        if (!rateLimiter.isExists()) {
            return 0;
        }
        return rateLimiter.availablePermits();
    }

    /**
     * 检查限流器是否存在
     *
     * @param key 限流器标识
     * @return 是否存在
     */
    public boolean exists(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_PREFIX + key);
        return rateLimiter.isExists();
    }

    /**
     * 获取或创建限流器
     *
     * @param key      限流器标识
     * @param rate     时间间隔内允许的请求数
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return 限流器实例
     */
    private RRateLimiter getOrCreateRateLimiter(String key, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_PREFIX + key);
        // trySetRate 只在限流器不存在时设置，已存在则不会修改配置
        rateLimiter.trySetRate(RateType.OVERALL, rate, interval, unit);
        return rateLimiter;
    }

}
