package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.mapper.PayOrderMapper;
import cn.projectan.strix.model.constant.DelayedQueueConst;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.dict.PayOrderStatus;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.model.other.module.pay.BasePayParam;
import cn.projectan.strix.model.other.module.pay.BasePayResult;
import cn.projectan.strix.service.PayHandlerService;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.utils.*;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    private final PayHandlerService payHandlerService;
    private final DelayedQueueUtil delayedQueueUtil;
    private final SynchronizedUtil synchronizedUtil;
    private final StrixPayStore strixPayStore;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createOrder(String configId, String title, BasePayParam param, String attach, Integer amount, Integer expireMin, String handlerId, Integer payType) {
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

        } catch (JsonProcessingException e) {
            throw new StrixException("支付数据序列化失败");
        }
        payOrder.setStatus(PayOrderStatus.UNPAID);
        payOrder.setTitle(title);
        payOrder.setExpireTime(LocalDateTime.now().plusMinutes(expireMin));
        payOrder.setAttach(attach);
        payOrder.setTotalAmount(amount);
        payOrder.setTotalPayAmount(0);
        payOrder.setTotalRefundAmount(0);
        payOrder.setCreateBy(SecurityUtils.getManagerId());
        payOrder.setUpdateBy(SecurityUtils.getManagerId());
        Assert.isTrue(save(payOrder), "创建订单失败");

        delayedQueueUtil.offer(DelayedQueueConst.PAY_ORDER_EXPIRE, payOrder.getId(), expireMin, TimeUnit.MINUTES);

        Map<String, String> responseMap = null;
        switch (payType) {
            case PayType.WAP -> responseMap = payClient.createWapPay(payOrder);
            case PayType.WEB -> responseMap = payClient.createWebPay(payOrder);
            case PayType.APP -> throw new StrixException("暂不支持APP端支付");
        }
        Assert.notNull(responseMap, "支付订单生成失败");

        return responseMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayResult(BasePayResult payResult) {
        PayOrderServiceImpl proxy = SpringUtil.getAopProxy(this);

        Assert.isTrue(payResult.getSuccess(), "支付结果非成功");
        Assert.hasText(payResult.getOrderId(), "支付结果订单号为空");

        synchronizedUtil.exec("PayOrder" + payResult.getOrderId(), () -> {
            PayOrder payOrder = proxy.getById(payResult.getOrderId());
            Assert.notNull(payOrder, "支付订单不存在");
            // 防止重复通知
            // 这里允许未支付和过期状态的订单处理支付成功通知
            StrixAssert.in(payOrder.getStatus(), "当前订单状态异常, 可能重复通知", PayOrderStatus.UNPAID, PayOrderStatus.EXPIRED);
            Assert.isTrue(payOrder.getTotalAmount().equals(payResult.getTotalAmount()), "支付金额不一致");

            payOrder.setStatus(PayOrderStatus.PAID);
            payOrder.setPayTime(LocalDateTime.now());
            payOrder.setTotalPayAmount(payResult.getTotalAmount());
            payOrder.setNotifyContent(payResult.getOriginalResult());

            Assert.isTrue(proxy.updateById(payOrder), "更新订单信息失败");

            // 移除订单过期队列
            delayedQueueUtil.remove(DelayedQueueConst.PAY_ORDER_EXPIRE, payOrder.getId());

            // 调用订单业务处理器
            payHandlerService.handleSuccess(payOrder.getHandlerId(), payOrder.getId());
        });
    }

    @Override
    public void handleExpired(String orderId) {
        Assert.hasText(orderId, "订单号为空");
        synchronizedUtil.exec("PayOrder" + orderId, () -> {
            UpdateWrapper<PayOrder> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderId);
            // 只处理未支付的订单 防止处理到已支付的订单
            updateWrapper.eq("status", PayOrderStatus.UNPAID);
            updateWrapper.set("status", PayOrderStatus.EXPIRED);
            this.update(updateWrapper);
        });
    }

}
