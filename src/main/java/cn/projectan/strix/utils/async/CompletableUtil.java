package cn.projectan.strix.utils.async;

import cn.projectan.strix.core.threadpool.CompletableThreadPool;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * @author 安炯奕
 * @date 2023/9/29 18:07
 */
public class CompletableUtil {

    private static final ThreadPoolTaskExecutor executor = CompletableThreadPool.getInstance();

    public static void allOf(Runnable... runnableArr) {
        CompletableFuture.allOf(
                Arrays.stream(runnableArr)
                        .map(runnable -> CompletableFuture.runAsync(runnable, executor))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

    public static void anyOf(Runnable... runnableArr) {
        CompletableFuture.anyOf(
                Arrays.stream(runnableArr)
                        .map(runnable -> CompletableFuture.runAsync(runnable, executor))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

}
