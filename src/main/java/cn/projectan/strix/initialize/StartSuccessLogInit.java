package cn.projectan.strix.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动成功 Banner
 *
 * @author 安炯奕
 * @date 2023/6/18 15:42
 */
@Slf4j
@Order(Integer.MAX_VALUE)
@Component
public class StartSuccessLogInit implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("""


                                                   __         .__
                                           _______/  |________|__|__  ___
                                          /  ___/\\   __\\_  __ \\  \\  \\/  /
                                          \\___ \\  |  |  |  | \\/  |>    <
                                         /____  > |__|  |__|  |__/__/\\_ \\
                                              \\/                       \\/

                                       ProjectAn Strix is started successfully!

                """);
    }

}
