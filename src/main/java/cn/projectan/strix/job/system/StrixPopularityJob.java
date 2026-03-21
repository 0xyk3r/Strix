package cn.projectan.strix.job.system;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.service.system.PopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 热度工具定时任务
 *
 * @author ProjectAn
 * @since 2023/9/29 0:12
 */
@Slf4j
@StrixJob
@Component("strixPopularityJob")
@RequiredArgsConstructor
public class StrixPopularityJob {

    private final PopularityService popularityService;

    /**
     * 保存热度数据到数据库
     */
    public void saveToDatabase() {
        popularityService.syncToDB();
    }

}
