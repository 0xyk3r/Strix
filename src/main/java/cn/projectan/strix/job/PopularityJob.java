package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.util.PopularityUtil;
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
@Component("popularityJob")
@RequiredArgsConstructor
public class PopularityJob {

    private final PopularityUtil popularityUtil;

    /**
     * 保存热度数据到数据库
     */
    public void saveToDatabase() {
        popularityUtil.syncToDB();
    }

}
