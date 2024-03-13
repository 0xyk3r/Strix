package cn.projectan.strix.initialize;

import cn.projectan.strix.core.module.oss.StrixOssConfig;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.service.OssConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strix OSS 初始化
 *
 * @author ProjectAn
 * @date 2021/5/2 17:20
 */
@Slf4j
@Order(10)
@Component
@ConditionalOnBean(StrixOssConfig.class)
@RequiredArgsConstructor
public class StrixOssInit implements ApplicationRunner {

    private final OssConfigService ossConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<OssConfig> ossConfigList = ossConfigService.list();

        ossConfigService.createInstance(ossConfigList);
    }

}
