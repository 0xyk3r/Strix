package cn.projectan.strix.initializer;

import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.model.db.PayConfig;
import cn.projectan.strix.service.PayConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strix Pay 初始化器
 *
 * @author ProjectAn
 * @date 2021/8/24 12:58
 */
@Slf4j
@Order(11)
@Component
@ConditionalOnBean(StrixPayStore.class)
@RequiredArgsConstructor
public class StrixPayInitializer implements ApplicationRunner {

    private final PayConfigService payConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<PayConfig> payConfigList = payConfigService.list();
        payConfigService.createInstance(payConfigList);
    }

}
