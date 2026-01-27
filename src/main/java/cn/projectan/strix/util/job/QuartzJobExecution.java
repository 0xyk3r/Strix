package cn.projectan.strix.util.job;

import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.util.reflect.InvokeUtil;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（允许并发执行）
 *
 * @author ProjectAn
 */
public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, Job job) {
        InvokeUtil.invokeMethod(job.getInvokeTarget());
    }

}
