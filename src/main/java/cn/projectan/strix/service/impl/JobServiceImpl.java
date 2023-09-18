package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.exception.StrixJobException;
import cn.projectan.strix.mapper.JobMapper;
import cn.projectan.strix.model.constant.JobConstants;
import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.model.dict.JobStatus;
import cn.projectan.strix.service.JobService;
import cn.projectan.strix.utils.job.CronUtil;
import cn.projectan.strix.utils.job.JobInvokeUtil;
import cn.projectan.strix.utils.job.ScheduleUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
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
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-07-30
 */
@Service
@ConditionalOnProperty(prefix = "strix.module", name = "job", havingValue = "true")
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private final Scheduler scheduler;

    @Autowired
    public JobServiceImpl(@Autowired(required = false) Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(String[] jobIds) throws SchedulerException {
        for (String jobId : jobIds) {
            Job job = getBaseMapper().selectById(jobId);
            Assert.isTrue(deleteJob(job), "删除任务失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Job job) throws SchedulerException {
        Integer status = job.getStatus();
        if (status == JobStatus.PAUSE) {
            Assert.isTrue(resumeJob(job), "切换任务状态失败");
        } else if (status == JobStatus.NORMAL) {
            Assert.isTrue(pauseJob(job), "切换任务状态失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(String id) throws SchedulerException {
        Job job = getBaseMapper().selectById(id);
        Assert.notNull(job, "任务不存在");
        // 组装参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(JobConstants.TASK_PROPERTIES, job);
        JobKey jobKey = ScheduleUtils.getJobKey(job.getId(), job.getGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey, dataMap);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertJob(Job job) throws SchedulerException, StrixJobException {
        job.setStatus(JobStatus.PAUSE);
        int rows = getBaseMapper().insert(job);
        if (rows > 0) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    @Override
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

    @Override
    public void checkJobLegal(Job job) {
        Assert.isTrue(CronUtil.isValid(job.getCronExpression()), "Cron表达式不正确");
        Assert.isTrue(!StringUtils.containsIgnoreCase(job.getInvokeTarget(), "rmi:"), "目标字符串不合法");
        Assert.isTrue(!StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), "ldap:", "ldaps:"), "目标字符串不合法");
        Assert.isTrue(!StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), "http://", "https://"), "目标字符串不合法");
        Assert.isTrue(!StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
                "org.springframework", "org.apache", "cn.projectan.strix.utils", "cn.projectan.strix.config"), "目标字符串不合法");
        Assert.isTrue(JobInvokeUtil.valid(job), "目标字符串不合法");
    }
}
