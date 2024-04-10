package cn.projectan.strix.utils.captcha;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Strix Captcha 缓存工具类
 *
 * @author ProjectAn
 * @date 2024/3/30 13:00
 */
@Slf4j
public final class StrixCaptchaCacheUtil {

    private static final Map<String, Object> CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 缓存最大个数
     */
    private static Integer CACHE_MAX_NUMBER = 1000;

    /**
     * 初始化
     *
     * @param cacheMaxNumber 缓存最大个数
     * @param second         定时任务 秒执行清除过期缓存
     */
    public static void init(int cacheMaxNumber, long second) {
        CACHE_MAX_NUMBER = cacheMaxNumber;
        if (second > 0L) {
            scheduledExecutor = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "thd-captcha-cache-clean"), new ThreadPoolExecutor.CallerRunsPolicy());
            scheduledExecutor.scheduleAtFixedRate(StrixCaptchaCacheUtil::refresh, 10, second, TimeUnit.SECONDS);
        }
    }

    private static ScheduledExecutorService scheduledExecutor;

    /**
     * 缓存刷新,清除过期数据
     */
    public static void refresh() {
        for (String key : CACHE_MAP.keySet()) {
            exists(key);
        }
    }


    public static void set(String key, String value, long expiresInSeconds) {
        // 设置阈值，达到即clear缓存
        if (CACHE_MAP.size() > CACHE_MAX_NUMBER * 2) {
            clear();
        }
        CACHE_MAP.put(key, value);
        if (expiresInSeconds > 0) {
            CACHE_MAP.put(key + "_HoldTime", System.currentTimeMillis() + expiresInSeconds * 1000); //缓存失效时间
        }
    }

    public static void delete(String key) {
        CACHE_MAP.remove(key);
        CACHE_MAP.remove(key + "_HoldTime");
    }

    public static boolean exists(String key) {
        Long cacheHoldTime = (Long) CACHE_MAP.get(key + "_HoldTime");
        if (cacheHoldTime == null || cacheHoldTime == 0L) {
            return false;
        }
        if (cacheHoldTime < System.currentTimeMillis()) {
            delete(key);
            return false;
        }
        return true;
    }


    public static String get(String key) {
        if (exists(key)) {
            return (String) CACHE_MAP.get(key);
        }
        return null;
    }

    /**
     * 删除所有缓存
     */
    public static void clear() {
        CACHE_MAP.clear();
    }
}
