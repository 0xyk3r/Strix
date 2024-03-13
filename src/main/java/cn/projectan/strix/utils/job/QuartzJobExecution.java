package cn.projectan.strix.utils.job;

import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.utils.InvokeUtil;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（允许并发执行）
 *
 * @author ProjectAn
 */
public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, Job job) throws Exception {
        InvokeUtil.invokeMethod(job.getInvokeTarget());
    }

}
