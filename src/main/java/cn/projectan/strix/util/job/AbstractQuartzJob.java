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

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) {
        Object o = context.getMergedJobDataMap().get(StrixJobConst.TASK_PROPERTIES);
        if (o instanceof Job job) {
            try {
                before(context, job);
                doExecute(context, job);
                after(context, job, null);
            } catch (Exception e) {
                log.error("任务执行异常  - ：", e);
                after(context, job, e);
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
        threadLocal.set(System.currentTimeMillis());
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    protected void after(JobExecutionContext context, Job job, Exception e) {
        Long startTime = threadLocal.get();
        threadLocal.remove();
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    protected abstract void doExecute(JobExecutionContext context, Job job);

}
