package cn.projectan.strix.util.job;

import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.util.InvokeUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（不允许并发执行）
 *
 * @author ProjectAn
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, Job job) throws Exception {
        InvokeUtil.invokeMethod(job.getInvokeTarget());
    }

}
