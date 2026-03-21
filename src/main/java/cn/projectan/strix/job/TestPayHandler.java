package cn.projectan.strix.job;

import cn.projectan.strix.core.module.pay.PayCallbackHandler;
import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.model.db.system.PayOrder;
import cn.projectan.strix.service.system.PayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024/4/13 下午11:02
 */
@Slf4j
@StrixJob
@Component("testPayHandler")
@RequiredArgsConstructor
public class TestPayHandler implements PayCallbackHandler {

    private final PayOrderService payOrderService;

    @Override
    public void onPaySuccess(String orderId) {
        log.info("Do job: `TestPayHandler.onPaySuccess` with orderId: {}", orderId);
        PayOrder payOrder = payOrderService.getById(orderId);
        log.info("onPaySuccess: {}", payOrder);
    }

    @Override
    public void onPayRefund(String orderId) {
        log.info("Do job: `TestPayHandler.onPayRefund` with orderId: {}", orderId);
        PayOrder payOrder = payOrderService.getById(orderId);
        log.info("onPayRefund: {}", payOrder);
    }

    @Override
    public void onPayTimeout(String orderId) {
        log.info("Do job: `TestPayHandler.onPayTimeout` with orderId: {}", orderId);
        PayOrder payOrder = payOrderService.getById(orderId);
        log.info("onPayTimeout: {}", payOrder);
    }

}
