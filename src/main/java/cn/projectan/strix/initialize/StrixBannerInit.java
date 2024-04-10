package cn.projectan.strix.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动成功 Banner
 *
 * @author ProjectAn
 * @date 2023/6/18 15:42
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class StrixBannerInit implements ApplicationRunner {

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
