package cn.projectan.strix.initializer;

import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.service.SmsConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strix SMS 初始化器
 *
 * @author ProjectAn
 * @since 2021/5/2 17:46
 */
@Slf4j
@Order(10)
@Component
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
public class StrixSmsInitializer implements ApplicationRunner {

    private final SmsConfigService smsConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<SmsConfig> smsConfigList = smsConfigService.list();
        smsConfigService.createInstance(smsConfigList);
    }
}
