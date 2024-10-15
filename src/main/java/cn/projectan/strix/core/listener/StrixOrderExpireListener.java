package cn.projectan.strix.core.listener;

import cn.projectan.strix.model.constant.DelayedQueueConst;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.util.DelayedQueueUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 订单超时处理监听器
 *
 * @author ProjectAn
 * @since 2024/4/14 上午12:32
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrixOrderExpireListener {

    private final PayOrderService payOrderService;
    private final DelayedQueueUtil delayedQueueUtil;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void listener() {
        executor.execute(() -> {
            RBlockingDeque<String> queue = delayedQueueUtil.getQueue(DelayedQueueConst.PAY_ORDER_EXPIRE);
            while (true) {
                try {
                    String orderId = queue.take();
                    payOrderService.handleExpired(orderId);
                } catch (InterruptedException e) {
                    log.error("订单超时处理监听器异常", e);
                }
            }
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }

}
