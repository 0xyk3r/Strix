package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemLog;

import java.util.concurrent.CompletableFuture;

/**
 * 异步系统日志服务
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
public interface AsyncSystemLogService {

    /**
     * 异步保存日志
     *
     * @param systemLog 系统日志
     * @return CompletableFuture
     */
    CompletableFuture<Void> saveAsync(SystemLog systemLog);

    /**
     * 定时批量保存日志
     * * 每5秒执行一次，或队列达到批次大小时触发
     */
    void batchSaveLogs();

    /**
     * 立即刷新队列中的所有日志到数据库（用于应用关闭时）
     */
    void flushAll();
}
