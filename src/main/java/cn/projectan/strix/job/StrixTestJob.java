package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/7/30 16:51
 */
@Slf4j
@StrixJob
@Component("strixTestJob")
public class StrixTestJob {

    public void testSomething() {
        log.info("Do job: `StrixTestJob.testSomething`");
    }

    public void testParams(String str, Integer i, Double d, Long l, Boolean b) {
        log.info("Do job: `StrixTestJob.testParams`, your params is: " + str + ", " + i + ", " + d + ", " + l + ", " + b);
    }

}
