package cn.projectan.strix.initialize;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.service.SmsConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/2 17:46
 */
@Slf4j
@Order(10)
@Component
@ConditionalOnBean(StrixSmsConfig.class)
public class StrixSmsInit implements ApplicationRunner {

    @Autowired
    private SmsConfigService smsConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<SmsConfig> smsConfigList = smsConfigService.list();

        smsConfigService.createSmsInstance(smsConfigList);
    }
}
