package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author 安炯奕
 * @date 2023/6/16 23:01
 */
@Slf4j
public class ThreadUtil {

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 停止线程池
     * 先使用 shutdown , 停止接收新任务并尝试完成所有已存在任务.
     * 如果超时, 则调用 shutdownNow, 取消在 workQueue 中 Pending 的任务, 并中断所有阻塞函数.
     * 如果仍然超时，则强制退出.
     * 另对在 shutdown 时线程本身被调用中断做了处理.
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                        log.warn("ThreadUtil: Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
