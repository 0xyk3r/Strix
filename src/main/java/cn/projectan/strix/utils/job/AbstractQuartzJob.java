package cn.projectan.strix.utils.job;

import cn.projectan.strix.model.constant.JobConstants;
import cn.projectan.strix.model.db.Job;
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
        Object o = context.getMergedJobDataMap().get(JobConstants.TASK_PROPERTIES);
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

//        final SysJobLog sysJobLog = new SysJobLog();
//        sysJobLog.setJobName(sysJob.getJobName());
//        sysJobLog.setJobGroup(sysJob.getJobGroup());
//        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
//        sysJobLog.setStartTime(startTime);
//        sysJobLog.setStopTime(new Date());
//        long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
//        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
//        if (e != null) {
//            sysJobLog.setStatus(Constants.FAIL);
//            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
//            sysJobLog.setExceptionInfo(errorMsg);
//        } else {
//            sysJobLog.setStatus(Constants.SUCCESS);
//        }
//
//        // 写入数据库当中
//        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, Job job) throws Exception;

}
