package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.PayHandlerMapper;
import cn.projectan.strix.model.db.PayHandler;
import cn.projectan.strix.service.PayHandlerService;
import cn.projectan.strix.util.InvokeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * Strix Pay 订单处理器 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-13
 */
@Service
public class PayHandlerServiceImpl extends ServiceImpl<PayHandlerMapper, PayHandler> implements PayHandlerService {

    @Override
    public void handleSuccess(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

    @Override
    public void handleRefund(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

    @Override
    public void handleTimeout(String id, String orderId) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, "支付处理器不存在");
        String invokeTarget = payHandler.getSuccessHandler().replace("{{ORDER_ID}}", orderId);
        InvokeUtil.invokeMethod(invokeTarget);
    }

}
