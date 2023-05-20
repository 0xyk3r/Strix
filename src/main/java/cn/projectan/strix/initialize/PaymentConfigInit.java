package cn.projectan.strix.initialize;

import cn.projectan.strix.config.GlobalPaymentConfig;
import cn.projectan.strix.model.db.PaymentConfig;
import cn.projectan.strix.service.PaymentConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/8/24 12:58
 */
@Slf4j
@Order(value = 11)
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "payment", havingValue = "true")
public class PaymentConfigInit implements ApplicationRunner {

    @Autowired
    private PaymentConfigService paymentConfigService;

    @Autowired
    private GlobalPaymentConfig globalPaymentConfig;

    @Override
    public void run(ApplicationArguments args) {
        List<PaymentConfig> paymentConfigList = paymentConfigService.list();

        for (PaymentConfig paymentConfig : paymentConfigList) {
            globalPaymentConfig.addInstance(paymentConfig.getId(), paymentConfig);
        }
    }

}
