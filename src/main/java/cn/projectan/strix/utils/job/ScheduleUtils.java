package cn.projectan.strix.utils.job;

import cn.projectan.strix.core.exception.StrixJobException;
import cn.projectan.strix.model.constant.JobConstants;
import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.model.dict.CommonSwitch;
import cn.projectan.strix.model.dict.JobMisfire;
import cn.projectan.strix.model.dict.JobStatus;
import cn.projectan.strix.utils.CronUtil;
import org.quartz.*;

/**
 * 定时任务工具类
 */
public class ScheduleUtils {

    /**
     * 获取 quartz 任务类
     *
     * @param job 执行计划
     * @return 具体执行任务类
     */
    private static Class<? extends org.quartz.Job> getQuartzJobClass(Job job) {
        boolean isConcurrent = job.getConcurrent() == CommonSwitch.ENABLE;
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    /**
     * 构建任务触发对象
     */
    public static TriggerKey getTriggerKey(String jobId, String jobGroup) {
        return TriggerKey.triggerKey(JobConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 构建任务键对象
     */
    public static JobKey getJobKey(String jobId, String jobGroup) {
        return JobKey.jobKey(JobConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, Job job) throws SchedulerException, StrixJobException {
        Class<? extends org.quartz.Job> jobClass = getQuartzJobClass(job);
        // 构建 quartz job 信息
        String jobId = job.getId();
        String jobGroup = job.getGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

        // 构建 cron 表达式调度器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 按 cron 表达式构建一个新的 trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(JobConstants.TASK_PROPERTIES, job);

        // 判断 quartz job 是否存在
        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            // 防止创建时发生未知问题 先删除 quartz job，重新创建
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        // 判断 quartz job 是否过期
        if (CronUtil.getNextExecution(job.getCronExpression()) != null) {
            // 执行调度任务
            scheduler.scheduleJob(jobDetail, trigger);
        }

        // 暂停任务
        if (job.getStatus() == JobStatus.PAUSE) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 设置定时任务策略
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(Job job, CronScheduleBuilder cb)
            throws StrixJobException {
        return switch (job.getMisfirePolicy()) {
            case JobMisfire.DEFAULT -> cb;
            case JobMisfire.IGNORE_MISFIRES -> cb.withMisfireHandlingInstructionIgnoreMisfires();
            case JobMisfire.FIRE_AND_PROCEED -> cb.withMisfireHandlingInstructionFireAndProceed();
            case JobMisfire.DO_NOTHING -> cb.withMisfireHandlingInstructionDoNothing();
            default -> throw new StrixJobException("The task misfire policy '" + job.getMisfirePolicy()
                    + "' cannot be used in cron schedule tasks", StrixJobException.Code.CONFIG_ERROR);
        };
    }

}
