package cn.projectan.strix.initialize;

import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.service.JobService;
import cn.projectan.strix.utils.job.ScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/7/30 16:22
 */
@Slf4j
@Order(100)
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "job", havingValue = "true")
@RequiredArgsConstructor
public class StrixJobInit implements ApplicationRunner {

    private final JobService jobService;
    private final Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) {
        try {
            scheduler.clear();
            List<Job> jobList = jobService.list();
            for (Job job : jobList) {
                ScheduleUtils.createScheduleJob(scheduler, job);
            }
        } catch (Exception e) {
            log.error("StrixJobInit run error", e);
        }
    }

}
