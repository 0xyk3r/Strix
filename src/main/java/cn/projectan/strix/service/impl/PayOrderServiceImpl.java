package cn.projectan.strix.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.mapper.PayOrderMapper;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.dict.PayOrderStatus;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.model.other.module.pay.PaymentData;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.utils.SecurityUtils;
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

    private final StrixPayStore strixPayStore;
    private final ObjectMapper objectMapper;

    /**
     * 生成支付订单
     *
     * @param configId 支付配置id
     * @param title    支付内容标题
     * @param data     支付参数
     * @param attach   支付回调参数
     * @param amount   支付总金额
     * @return 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createOrder(String configId, String title, PaymentData data, String attach, Integer amount, Integer payType) {
        StrixPayClient payClient = strixPayStore.getInstance(configId);
        Assert.notNull(payClient, "收款配置异常, 创建订单失败");
        Assert.isTrue(PayType.valid(payType), "支付类型不合法");

        PayOrder payOrder = new PayOrder();
        payOrder.setPaymentConfigId(configId);
        payOrder.setPaymentConfigName(payClient.getConfigName());
        payOrder.setPaymentMethod(payClient.getPlatform());
        payOrder.setPaymentData("{}");
        try {
            if (payClient.getPlatform() == PayPlatform.WX_PAY) {
                payOrder.setPaymentData(objectMapper.writeValueAsString(data));
            }

        } catch (JsonProcessingException e) {
            throw new StrixException("支付数据序列化失败");
        }
        // TODO 枚举
        payOrder.setPaymentStatus(1);
        payOrder.setPaymentTitle(title);
        payOrder.setPaymentAttach(attach);
        payOrder.setTotalAmount(amount);
        payOrder.setTotalPayAmount(0);
        payOrder.setTotalRefundAmount(0);
        payOrder.setCreateBy(SecurityUtils.getManagerId());
        payOrder.setUpdateBy(SecurityUtils.getManagerId());
        Assert.isTrue(save(payOrder), "创建订单失败");

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
    public void savePayResult(Map<String, Object> payResult) throws Exception {
        String tradeState = MapUtil.getStr(payResult, "trade_state");
        Assert.isTrue("SUCCESS".equalsIgnoreCase(tradeState), "支付回调状态为支付失败");

        String outTradeNo = MapUtil.getStr(payResult, "out_trade_no");
        Assert.hasText(outTradeNo, "回调未获取到商户订单号");
        PayOrder payOrder = getBaseMapper().selectById(outTradeNo);
        Assert.notNull(payOrder, "未查询到商户订单号对应订单");

        Integer paymentOrderTotalAmount = payOrder.getTotalAmount();
        Map<String, Object> amountMap = MapUtil.get(payResult, "amount", new cn.hutool.core.lang.TypeReference<>() {
        });
        Integer payerTotal = MapUtil.getInt(amountMap, "payer_total");
        Assert.isTrue(paymentOrderTotalAmount.equals(payerTotal), "实际支付金额与订单金额不一致");

        Assert.isTrue(PayOrderStatus.UNPAID == payOrder.getPaymentStatus(), "当前订单状态不支持更改支付结果，可能重复通知");

        payOrder.setPaymentStatus(PayOrderStatus.PAID);
        payOrder.setPaymentTime(LocalDateTime.now());
        payOrder.setPaymentResponse(objectMapper.writeValueAsString(payResult));
        payOrder.setTotalPayAmount(payerTotal);

        Assert.isTrue(getBaseMapper().updateById(payOrder) > 0, "保存支付结果失败");
    }
}
