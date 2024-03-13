package cn.projectan.strix.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.config.GlobalPaymentConfig;
import cn.projectan.strix.mapper.PaymentOrderMapper;
import cn.projectan.strix.model.db.PaymentOrder;
import cn.projectan.strix.model.dict.PaymentOrderStatus;
import cn.projectan.strix.model.wechat.payment.WxPayConfig;
import cn.projectan.strix.service.PaymentOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.RequestMethodEnum;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.core.utils.DateTimeZoneUtil;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxDomainEnum;
import com.ijpay.wxpay.enums.v3.BasePayApiEnum;
import com.ijpay.wxpay.model.v3.Amount;
import com.ijpay.wxpay.model.v3.Payer;
import com.ijpay.wxpay.model.v3.UnifiedOrderModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    private final GlobalPaymentConfig globalPaymentConfig;
    private final ObjectMapper objectMapper;

    /**
     * 生成支付订单
     *
     * @param paymentConfigId 支付配置id
     * @param title           支付内容标题
     * @param attach          支付回调参数
     * @param totalAmount     支付总金额
     * @return 订单信息
     */
    private PaymentOrder createOrder(String paymentConfigId, String title, String paymentData, String attach, Integer totalAmount) {
        WxPayConfig wxPayConfig = (WxPayConfig) globalPaymentConfig.getInstance(paymentConfigId);
        Assert.notNull(wxPayConfig, "支付配置无效，无法创建订单");

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentConfigId(paymentConfigId);
        paymentOrder.setPaymentConfigName(wxPayConfig.getName());
        paymentOrder.setPaymentMethod(wxPayConfig.getPlatform());
        paymentOrder.setPaymentData(paymentData);
        // TODO 枚举
        paymentOrder.setPaymentStatus(1);
        paymentOrder.setPaymentTitle(title);
        paymentOrder.setPaymentAttach(attach);
        paymentOrder.setTotalAmount(totalAmount);
        paymentOrder.setTotalPayAmount(0);
        paymentOrder.setTotalRefundAmount(0);
        paymentOrder.setCreateBy("DevTest");
        paymentOrder.setUpdateBy("DevTest");
        Assert.isTrue(save(paymentOrder), "创建订单失败");

        return paymentOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> genWxPayJsApiOrder(String paymentConfigId, String title, String paymentData, String attach, Integer totalAmount) {
        PaymentOrder paymentOrder = createOrder(paymentConfigId, title, paymentData, attach, totalAmount);

        WxPayConfig wxPayConfig = (WxPayConfig) globalPaymentConfig.getInstance(paymentOrder.getPaymentConfigId());
        Assert.notNull(wxPayConfig, "支付配置无效，无法创建订单");
        try {
            Map<String, String> pd = objectMapper.readValue(paymentOrder.getPaymentData(), new TypeReference<>() {
            });

            String callbackUrl = wxPayConfig.getCallbackUrl().replace("{mchName}", paymentOrder.getPaymentConfigId());
            System.out.println(callbackUrl);
            log.info(callbackUrl);

            String expireTime = DateTimeZoneUtil.dateToTimeZone(System.currentTimeMillis() + (1000 * 60 * 30));
            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(wxPayConfig.getV3AppId())
                    .setMchid(wxPayConfig.getMchId())
                    .setDescription(paymentOrder.getPaymentTitle())
                    .setOut_trade_no(paymentOrder.getId())
                    .setTime_expire(expireTime)
                    .setAttach(paymentOrder.getPaymentAttach())
                    .setNotify_url(callbackUrl)
                    .setAmount(new Amount().setTotal(paymentOrder.getTotalAmount()))
                    .setPayer(new Payer().setOpenid(pd.get("openId")));

            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.getDomain(),
                    BasePayApiEnum.JS_API_PAY.getUrl(),
                    wxPayConfig.getMchId(),
                    wxPayConfig.getSerialNumber(),
                    null,
                    wxPayConfig.getV3KeyPath(),
                    JSONUtil.toJsonStr(unifiedOrderModel)
            );

            Assert.isTrue(response.getStatus() == 200, "微信JSAPI支付信息请求结果异常： " + response.getBody());
            // 根据证书序列号查询对应的证书来验证签名结果
            boolean verifySignature = WxPayKit.verifySignature(response, wxPayConfig.getV3PlatformCertPath());
            Assert.isTrue(verifySignature, "校验微信JSAPI支付请求响应签名失败： " + response.getBody());

            Map<String, Object> jsonResponse = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            String prepayId = MapUtil.getStr(jsonResponse, "prepay_id");

            return WxPayKit.jsApiCreateSign(wxPayConfig.getV3AppId(), prepayId, wxPayConfig.getV3KeyPath());
        } catch (Exception e) {
            log.error("微信JSAPI支付信息生成时发生异常", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return null;
        }
    }

    @Override
    public void savePayResult(Map<String, Object> payResult) throws Exception {
        String tradeState = MapUtil.getStr(payResult, "trade_state");
        Assert.isTrue("SUCCESS".equalsIgnoreCase(tradeState), "支付回调状态为支付失败");

        String outTradeNo = MapUtil.getStr(payResult, "out_trade_no");
        Assert.hasText(outTradeNo, "回调未获取到商户订单号");
        PaymentOrder paymentOrder = getBaseMapper().selectById(outTradeNo);
        Assert.notNull(paymentOrder, "未查询到商户订单号对应订单");

        Integer paymentOrderTotalAmount = paymentOrder.getTotalAmount();
        Map<String, Object> amountMap = MapUtil.get(payResult, "amount", new cn.hutool.core.lang.TypeReference<>() {
        });
        Integer payerTotal = MapUtil.getInt(amountMap, "payer_total");
        Assert.isTrue(paymentOrderTotalAmount.equals(payerTotal), "实际支付金额与订单金额不一致");

        Assert.isTrue(PaymentOrderStatus.UNPAID == paymentOrder.getPaymentStatus(), "当前订单状态不支持更改支付结果，可能重复通知");

        paymentOrder.setPaymentStatus(PaymentOrderStatus.PAID);
        paymentOrder.setPaymentTime(LocalDateTime.now());
        paymentOrder.setPaymentResponse(objectMapper.writeValueAsString(payResult));
        paymentOrder.setTotalPayAmount(payerTotal);

        Assert.isTrue(getBaseMapper().updateById(paymentOrder) > 0, "保存支付结果失败");
    }
}
