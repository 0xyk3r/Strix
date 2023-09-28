package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.utils.PopularityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/9/29 0:12
 */
@Slf4j
@StrixJob
@Component("popularityJob")
@RequiredArgsConstructor
public class PopularityJob {

    private final PopularityUtil popularityUtil;

    public void saveToDatabase() {
        popularityUtil.saveToDatabase();
    }

}
