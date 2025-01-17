package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.model.db.SystemConfig;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.service.SystemConfigService;
import cn.projectan.strix.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 定时任务测试方法
 *
 * @author ProjectAn
 * @since 2023/7/30 16:51
 */
@Slf4j
@StrixJob
@Component("strixTestJob")
@RequiredArgsConstructor
public class StrixTestJob {

    private final SystemConfigService systemConfigService;

    public void testSomething() {
        log.info("Do job: `StrixTestJob.testSomething`");
    }

    public void testParams(String str, Integer i, Double d, Long l, Boolean b) {
        log.info("Do job: `StrixTestJob.testParams`, your params is: {}, {}, {}, {}, {}", str, i, d, l, b);
    }

    public void testUpdateDB(String str) {
        log.info("Do job: `StrixTestJob.testUpdateDB`, your params is: {}", str);
        SystemConfig testConfig = systemConfigService.getById("DevTest");
        testConfig.setValue(str);
        systemConfigService.updateById(testConfig);
    }

    public boolean testCheckSystemManager() {
        String loginManagerName = Optional.ofNullable(SecurityUtils.getSystemManager()).map(SystemManager::getNickname).orElse(null);
        log.info("Do job: `StrixTestJob.testCheckSystemManager` {}", loginManagerName);
        return "anjiongyi".equals(loginManagerName);
    }

}
