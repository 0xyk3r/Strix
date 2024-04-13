package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.service.PayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2024/4/13 下午11:02
 */
@Slf4j
@StrixJob
@Component("testPayHandler")
@RequiredArgsConstructor
public class TestPayHandler {

    private final PayOrderService payOrderService;

    public void handleSuccess(String orderId) {
        log.info("Do job: `TestPayHandler.handleSuccess` with orderId: {}", orderId);
        PayOrder payOrder = payOrderService.getById(orderId);
        log.info("PayOrder: {}", payOrder);
    }

}
