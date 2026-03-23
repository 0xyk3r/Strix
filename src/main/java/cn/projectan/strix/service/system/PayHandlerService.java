package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.pay.PayCallbackHandler;
import cn.projectan.strix.mapper.system.PayHandlerMapper;
import cn.projectan.strix.model.db.system.PayHandler;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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

    private final ApplicationContext applicationContext;

    /**
     * 处理成功
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleSuccess(String id, String orderId) {
        PayCallbackHandler handler = resolveHandler(id);
        handler.onPaySuccess(orderId);
    }

    /**
     * 处理退款
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleRefund(String id, String orderId) {
        PayCallbackHandler handler = resolveHandler(id);
        handler.onPayRefund(orderId);
    }

    /**
     * 处理超时
     *
     * @param id      订单处理器 id
     * @param orderId 订单 id
     */
    public void handleTimeout(String id, String orderId) {
        PayCallbackHandler handler = resolveHandler(id);
        handler.onPayTimeout(orderId);
    }

    private PayCallbackHandler resolveHandler(String id) {
        PayHandler payHandler = this.getById(id);
        Assert.notNull(payHandler, I18nUtil.notFound("field.payHandler"));
        String beanName = payHandler.getHandler();
        Assert.hasText(beanName, I18nUtil.notFound("field.payCallbackHandler"));
        Object bean = applicationContext.getBean(beanName);
        Assert.isInstanceOf(PayCallbackHandler.class, bean,
                "Bean '" + beanName + "' 未实现 PayCallbackHandler 接口");
        return (PayCallbackHandler) bean;
    }

}
