package cn.projectan.strix.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/6/18 15:42
 */
@Slf4j
@Order(Integer.MAX_VALUE)
@Component
public class StartSuccessLogInit implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("""

                ##################################################

                     ProjectAn Strix is started successfully!

                ##################################################
                """);
    }

}
