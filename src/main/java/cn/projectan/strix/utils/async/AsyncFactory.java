package cn.projectan.strix.utils.async;

import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.service.SystemLogService;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.ip.IpLocationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;

/**
 * 异步工厂
 *
 * @author ProjectAn
 * @since 2023/6/16 23:43
 */
@Slf4j
public class AsyncFactory {

    /**
     * 保存操作日志记录
     *
     * @param systemLog 操作日志信息
     * @return 任务task
     */
    public static TimerTask saveSystemLog(final SystemLog systemLog) {
        return new TimerTask() {
            @Override
            public void run() {
                systemLog.setClientLocation(IpLocationUtil.getLocation(systemLog.getClientIp()));
                SpringUtil.getBean(SystemLogService.class).save(systemLog);
            }
        };
    }

}
