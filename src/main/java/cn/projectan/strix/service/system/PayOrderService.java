package cn.projectan.strix.service.system;

import cn.projectan.strix.core.delayedtask.DelayedTaskManager;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.mapper.system.PayOrderMapper;
import cn.projectan.strix.model.constant.DelayedTaskConst;
import cn.projectan.strix.model.db.system.PayOrder;
import cn.projectan.strix.model.dict.system.PayOrderStatus;
import cn.projectan.strix.model.dict.system.PayPlatform;
import cn.projectan.strix.model.dict.system.PayType;
import cn.projectan.strix.model.other.system.module.pay.BasePayParam;
import cn.projectan.strix.model.other.system.module.pay.BasePayResult;
import cn.projectan.strix.util.async.SynchronizedUtil;
import cn.projectan.strix.util.tool.StrixAssert;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Strix Pay 订单 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderService extends ServiceImpl<PayOrderMapper, PayOrder> {

    private final PayHandlerService payHandlerService;
    private final DelayedTaskManager delayedTaskManager;
    private final SynchronizedUtil synchronizedUtil;
    private final StrixPayStore strixPayStore;
    private final ObjectMapper objectMapper;

    /**
     * 生成支付订单
     *
     * @param configId  支付配置id
     * @param title     支付内容标题
     * @param param     支付参数
     * @param attach    支付回调参数
     * @param amount    支付总金额
     * @param expireMin 过期时间(分钟)
     * @param handlerId 支付处理器id
     * @param payType   支付类型
     * @return 订单信息
     * @see PayType 支付类型
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createOrder(String configId, String title, BasePayParam param, String attach, Long amount, Integer expireMin, String handlerId, Short payType) {
        StrixPayClient payClient = strixPayStore.getInstance(configId);
        Assert.notNull(payClient, "收款配置异常, 创建订单失败");
        Assert.isTrue(PayType.valid(payType), "支付类型不合法");

        PayOrder payOrder = new PayOrder();
        payOrder.setConfigId(configId);
        payOrder.setPlatform(payClient.getPlatform());
        payOrder.setHandlerId(handlerId);
        payOrder.setParams("{}");
        try {
            if (payClient.getPlatform() == PayPlatform.WX_PAY) {
                payOrder.setParams(objectMapper.writeValueAsString(param));
            }

        } catch (JacksonException e) {
            throw new StrixException("支付数据序列化失败");
        }
        payOrder.setStatus(PayOrderStatus.UNPAID);
        payOrder.setTitle(title);
        payOrder.setExpireTime(LocalDateTime.now().plusMinutes(expireMin));
        payOrder.setAttach(attach);
        payOrder.setTotalAmount(amount);
        payOrder.setTotalPayAmount(0L);
        payOrder.setTotalRefundAmount(0L);
        Assert.isTrue(save(payOrder), "创建订单失败");

        delayedTaskManager.schedule(DelayedTaskConst.PAY_ORDER_EXPIRE, payOrder.getId(), expireMin, TimeUnit.MINUTES);

        Map<String, String> responseMap = null;
        switch (payType) {
            case PayType.WAP -> responseMap = payClient.createWapPay(payOrder);
            case PayType.WEB -> responseMap = payClient.createWebPay(payOrder);
            case PayType.APP -> throw new StrixException("暂不支持APP端支付");
        }
        Assert.notNull(responseMap, "支付订单生成失败");

        return responseMap;
    }

    /**
     * 处理支付结果
     *
     * @param payResult 支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePayResult(BasePayResult payResult) {
        Assert.isTrue(payResult.getSuccess(), "支付结果非成功");
        Assert.hasText(payResult.getOrderId(), "支付结果订单号为空");

        synchronizedUtil.exec("PayOrder" + payResult.getOrderId(), () -> {
            PayOrder payOrder = getById(payResult.getOrderId());
            Assert.notNull(payOrder, "支付订单不存在");
            // 防止重复通知
            // 这里允许未支付和过期状态的订单处理支付成功通知
            StrixAssert.in(payOrder.getStatus(), "当前订单状态异常, 可能重复通知", PayOrderStatus.UNPAID, PayOrderStatus.EXPIRED);
            Assert.isTrue(payOrder.getTotalAmount().equals(payResult.getTotalAmount()), "支付金额不一致");

            payOrder.setStatus(PayOrderStatus.PAID);
            payOrder.setPayTime(LocalDateTime.now());
            payOrder.setTotalPayAmount(payResult.getTotalAmount());
            payOrder.setNotifyContent(payResult.getOriginalResult());

            Assert.isTrue(updateById(payOrder), "更新订单信息失败");

            // 移除订单过期队列
            delayedTaskManager.cancel(DelayedTaskConst.PAY_ORDER_EXPIRE, payOrder.getId());

            // 调用订单业务处理器
            payHandlerService.handleSuccess(payOrder.getHandlerId(), payOrder.getId());
        });
    }

    /**
     * 处理订单过期
     *
     * @param orderId 订单id
     */
    public void handleExpired(String orderId) {
        Assert.hasText(orderId, "订单号为空");
        synchronizedUtil.exec("PayOrder" + orderId, () -> {
            // 只处理未支付的订单 防止处理到已支付的订单
            lambdaUpdate()
                    .eq(PayOrder::getId, orderId)
                    .eq(PayOrder::getStatus, PayOrderStatus.UNPAID)
                    .set(PayOrder::getStatus, PayOrderStatus.EXPIRED)
                    .update();
        });
    }

}
