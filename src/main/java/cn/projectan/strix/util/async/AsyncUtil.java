package cn.projectan.strix.util.async;

import cn.projectan.strix.util.SpringUtil;
import cn.projectan.strix.util.ThreadUtil;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 异步工具类
 *
 * @author ProjectAn
 * @since 2023/6/16 22:56
 */
public class AsyncUtil {

    /**
     * 延迟时间 单位ms
     */
    private final int OPERATE_DELAY_TIME = 10;

    /**
     * 异步任务调度线程池
     */
    private final ScheduledExecutorService executor = SpringUtil.getBean("strixScheduledExecutor");

    private AsyncUtil() {
    }

    private static final AsyncUtil INSTANCE = new AsyncUtil();

    public static AsyncUtil instance() {
        return INSTANCE;
    }

    /**
     * 执行任务
     *
     * @param task 任务
     */
    public void execute(TimerTask task) {
        executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止异步任务调度线程池
     */
    public void shutdown() {
        ThreadUtil.shutdownAndAwaitTermination(executor);
    }

}
