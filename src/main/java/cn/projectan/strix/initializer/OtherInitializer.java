package cn.projectan.strix.initializer;

import cn.projectan.strix.util.ip.IpLocationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 其他功能初始化器
 *
 * @author ProjectAn
 * @since 2023/6/18 15:42
 */
@Slf4j
@Order(100000000)
@Component
public class OtherInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 初始化 IP 地理位置数据库
        IpLocationUtil.getLocation("127.0.0.1");
    }

}
