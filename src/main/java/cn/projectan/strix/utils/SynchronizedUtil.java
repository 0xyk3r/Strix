package cn.projectan.strix.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步操作工具类
 *
 * @author ProjectAn
 * @date 2022/4/4 19:24
 */
@Component
public class SynchronizedUtil {

    private final Map<String, ReentrantLock> mutexCache = new ConcurrentHashMap<>();

    public void exec(String key, Runnable runnable) {
        ReentrantLock mutex = null;
        ReentrantLock mutexInCache;
        do {
            if (mutex != null) {
                mutex.unlock();
            }

            mutex = mutexCache.computeIfAbsent(key, k -> new ReentrantLock());

            mutex.lock();
            mutexInCache = mutexCache.get(key);
        } while (mutexInCache == null || mutex != mutexInCache);

        try {
            runnable.run();
        } finally {
            if (mutex.getQueueLength() == 0) {
                mutexCache.remove(key);
            }
            mutex.unlock();
        }
    }

}
