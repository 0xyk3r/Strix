package cn.projectan.strix.service.system;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.exception.StrixJobException;
import cn.projectan.strix.mapper.system.JobMapper;
import cn.projectan.strix.model.constant.system.StrixJobConst;
import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.model.dict.system.JobStatus;
import cn.projectan.strix.util.common.CronUtil;
import cn.projectan.strix.util.job.ScheduleUtils;
import cn.projectan.strix.util.reflect.InvokeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * <p>
 * Strix 定时任务 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-07-30
 */
@Service
@ConditionalOnProperty(prefix = "strix.module", name = "job", havingValue = "true")
public class JobService extends ServiceImpl<JobMapper, Job> {

    private final Scheduler scheduler;

    @Autowired
    public JobService(@Autowired(required = false) Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean pauseJob(Job job) throws SchedulerException {
        String jobId = job.getId();
        String jobGroup = job.getGroup();
        job.setStatus(JobStatus.PAUSE);
        int rows = getBaseMapper().updateById(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows > 0;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean resumeJob(Job job) throws SchedulerException {
        String jobId = job.getId();
        String jobGroup = job.getGroup();
        job.setStatus(JobStatus.NORMAL);
        int rows = getBaseMapper().updateById(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows > 0;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(Job job) throws SchedulerException {
        String jobId = job.getId();
        String jobGroup = job.getGroup();
        int rows = getBaseMapper().deleteById(job);
        if (rows > 0) {
            Assert.isTrue(scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup)), "删除任务失败");
        }
        return rows > 0;
    }

    /**
     * 批量删除调度信息
     *
     * @param jobIds 需要删除的任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(String[] jobIds) throws SchedulerException {
        for (String jobId : jobIds) {
            Job job = getBaseMapper().selectById(jobId);
            Assert.isTrue(deleteJob(job), "删除任务失败");
        }
    }

    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Job job) throws SchedulerException {
        Integer status = job.getStatus();
        if (status == JobStatus.PAUSE) {
            Assert.isTrue(resumeJob(job), "切换任务状态失败");
        } else if (status == JobStatus.NORMAL) {
            Assert.isTrue(pauseJob(job), "切换任务状态失败");
        }
    }

    /**
     * 立即运行任务
     *
     * @param id 调度ID
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean run(String id) throws SchedulerException {
        Job job = getBaseMapper().selectById(id);
        Assert.notNull(job, "任务不存在");
        // 组装参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(StrixJobConst.TASK_PROPERTIES, job);
        JobKey jobKey = ScheduleUtils.getJobKey(job.getId(), job.getGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey, dataMap);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 新增任务
     *
     * @param job 调度信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertJob(Job job) throws SchedulerException, StrixJobException {
        job.setStatus(JobStatus.PAUSE);
        int rows = getBaseMapper().insert(job);
        if (rows > 0) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 更新任务
     *
     * @param job 调度信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(Job job) throws SchedulerException, StrixJobException {
        String originalJobId = job.getId();
        String originalJobGroup = job.getGroup();
        int rows = getBaseMapper().updateById(job);
        if (rows > 0) {
            JobKey jobKey = ScheduleUtils.getJobKey(originalJobId, originalJobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 检查任务信息是否合法，不合法则抛出异常
     *
     * @param job 调度信息
     */
    public void checkJobLegal(Job job) {
        Assert.isTrue(CronUtil.isValid(job.getCronExpression()), "Cron表达式不正确");
        Assert.isTrue(!StrUtil.containsIgnoreCase(job.getInvokeTarget(), "rmi:"), "目标字符串不合法");
        //noinspection SpellCheckingInspection
        Assert.isTrue(!StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), "ldap:", "ldaps:"), "目标字符串不合法");
        //noinspection HttpUrlsUsage
        Assert.isTrue(!StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), "http://", "https://"), "目标字符串不合法");
        Assert.isTrue(!StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
                "org.springframework", "org.apache", "cn.projectan.strix.utils", "cn.projectan.strix.config"), "目标字符串不合法");
        Assert.isTrue(InvokeUtil.valid(job.getInvokeTarget()), "目标字符串不合法");
    }

}
