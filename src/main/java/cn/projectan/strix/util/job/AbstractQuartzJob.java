package cn.projectan.strix.util.job;

import cn.projectan.strix.model.constant.system.StrixJobConst;
import cn.projectan.strix.model.db.system.Job;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * quartz 抽象类
 *
 * @author ProjectAn
 */
@Slf4j
public abstract class AbstractQuartzJob implements org.quartz.Job {

    @Override
    public void execute(JobExecutionContext context) {
        Object o = context.getMergedJobDataMap().get(StrixJobConst.TASK_PROPERTIES);
        if (o instanceof Job job) {
            long startTime = System.currentTimeMillis();
            try {
                before(context, job);
                doExecute(context, job);
                after(context, job, null, startTime);
            } catch (Exception e) {
                log.error("任务执行异常  - ：", e);
                after(context, job, e, startTime);
            }
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    protected void before(JobExecutionContext context, Job job) {
        // 子类可覆写以执行前置逻辑
    }

    /**
     * 执行后
     *
     * @param context   工作执行上下文对象
     * @param job       系统计划任务
     * @param e         异常（正常执行时为 null）
     * @param startTime 任务开始时间戳（毫秒）
     */
    protected void after(JobExecutionContext context, Job job, Exception e, long startTime) {
        long spend = System.currentTimeMillis() - startTime;
        log.debug("任务执行耗时: {}ms", spend);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    protected abstract void doExecute(JobExecutionContext context, Job job);

}
