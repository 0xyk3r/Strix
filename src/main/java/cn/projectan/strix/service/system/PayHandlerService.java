package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.PayHandlerMapper;
import cn.projectan.strix.model.db.system.PayHandler;
import cn.projectan.strix.util.reflect.InvokeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * Strix Pay 订单处理器 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayHandlerService extends ServiceImpl<PayHandlerMapper, PayHandler> {

    /**
     * 处理成功
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleSuccess(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

    /**
     * 处理失败
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleRefund(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

    /**
     * 处理超时
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleTimeout(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

}
