package cn.projectan.strix.core.delayedtask.initializer;

import cn.projectan.strix.core.delayedtask.DelayedTaskManager;
import cn.projectan.strix.model.constant.DelayedTaskConst;
import cn.projectan.strix.service.PayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 支付模块延迟任务初始化器
 * 负责注册订单过期相关的延迟任务消费者
 *
 * @author ProjectAn
 * @since 2024-12-18
 */
@Slf4j
@Order(50)
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "pay", havingValue = "true")
@RequiredArgsConstructor
public class PayDelayedTaskInitializer implements ApplicationRunner {

    private final DelayedTaskManager delayedTaskManager;
    private final PayOrderService payOrderService;

    /**
     * 订单过期扫描间隔（秒）
     * 订单过期需要快速响应，设置为 1 秒扫描一次
     */
    private static final long SCAN_INTERVAL_SECONDS = 1L;

    @Override
    public void run(ApplicationArguments args) {
        registerPayOrderExpireConsumer();
        log.info("Strix Pay: 支付订单超时检查任务初始化完成.");
    }

    /**
     * 注册订单过期消费者
     */
    private void registerPayOrderExpireConsumer() {
        delayedTaskManager.registerConsumer(
                DelayedTaskConst.PAY_ORDER_EXPIRE,
                orderId -> {
                    try {
                        payOrderService.handleExpired(orderId);
                    } catch (Exception e) {
                        log.error("Failed to handle expired order: {}", orderId, e);
                    }
                },
                SCAN_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

}
