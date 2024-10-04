package cn.projectan.strix.utils.async;

import cn.projectan.strix.core.threadpool.CompletableThreadPool;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 并行执行工具类
 * <p>
 * 用于并行执行多个任务
 *
 * @author ProjectAn
 * @since 2023/9/29 18:07
 */
public class ParallelExecution {

    private static final ThreadPoolTaskExecutor executor = CompletableThreadPool.getInstance();

    /**
     * 并行执行多个任务，等待所有任务执行完毕后再继续执行后续代码
     *
     * @param runnableArr 任务数组
     */
    public static void allOf(Runnable... runnableArr) {
        CompletableFuture.allOf(
                Arrays.stream(runnableArr)
                        .map(runnable -> CompletableFuture.runAsync(runnable, executor))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

    /**
     * 并行执行多个任务，只要有一个任务执行完毕就继续执行后续代码
     *
     * @param runnableArr 任务数组
     */
    public static void anyOf(Runnable... runnableArr) {
        CompletableFuture.anyOf(
                Arrays.stream(runnableArr)
                        .map(runnable -> CompletableFuture.runAsync(runnable, executor))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

}
