package cn.projectan.strix.core.module.pay;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.other.module.pay.BasePayResult;
import cn.projectan.strix.model.other.module.pay.alipay.AlipayPayConfig;
import cn.projectan.strix.utils.Arithmetic;
import cn.projectan.strix.utils.CertUtil;
import cn.projectan.strix.utils.ServletUtils;
import cn.projectan.strix.utils.SpringUtil;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.alipay.AliPayApi;
import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝 Pay 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 2:18
 */
@Slf4j
public class AlipayPayClient extends StrixPayClient {

    protected final AlipayPayConfig config;
    protected final AliPayApiConfig apiConfig;
    private final ObjectMapper objectMapper;

    public AlipayPayClient(AlipayPayConfig config) {
        super();
        Assert.notNull(config, "Strix Pay: 初始化支付宝支付服务实例失败. (配置信息为空)");
        this.config = config;
        this.objectMapper = SpringUtil.getBean(ObjectMapper.class);
        try {
            this.apiConfig = AliPayApiConfig.builder()
                    .setAppId(config.getAppId())
                    .setAliPayPublicKey(config.getPublicKey())
                    .setCharset("UTF-8")
                    .setPrivateKey(config.getPrivateKey())
                    .setServiceUrl(config.getServerUrl())
                    .setSignType("RSA2")
                    .buildByCertContent(
                            CertUtil.getCertContent(config.getAppCertPath()),
                            CertUtil.getCertContent(config.getAliPayCertPath()),
                            CertUtil.getCertContent(config.getAliPayRootCertPath())
                    );
            AliPayApiConfigKit.setThreadLocalAliPayApiConfig(apiConfig);
        } catch (Exception e) {
            throw new RuntimeException("Strix Pay: 初始化支付宝支付服务实例失败. (配置信息错误)", e);
        }
    }

    @Override
    public String getConfigId() {
        return config.getId();
    }

    @Override
    public String getConfigName() {
        return config.getName();
    }

    @Override
    public int getPlatform() {
        return PayPlatform.ALI_PAY;
    }

    @Override
    public Map<String, String> createWapPay(PayOrder payOrder) {
        AliPayApiConfigKit.setThreadLocalAliPayApiConfig(apiConfig);

        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(payOrder.getId());
        model.setSubject(payOrder.getTitle());
        model.setTotalAmount(Arithmetic.div(payOrder.getTotalAmount(), 100, 2) + "");
        model.setPassbackParams(payOrder.getAttach());
        model.setProductCode("QUICK_WAP_PAY");
        model.setTimeExpire(payOrder.getExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            HttpServletResponse response = ServletUtils.getResponse();
            AliPayApi.wapPay(response, model, config.getReturnUrl(), config.getNotifyUrl());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Strix Pay: 支付宝支付下单失败. (支付宝支付下单异常)", e);
        }
        return null;
    }

    @Override
    public Map<String, String> createWebPay(PayOrder payOrder) {
        AliPayApiConfigKit.setThreadLocalAliPayApiConfig(apiConfig);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(payOrder.getId());
        model.setSubject(payOrder.getTitle());
        model.setTotalAmount(Arithmetic.div(payOrder.getTotalAmount(), 100, 2) + "");
        model.setPassbackParams(payOrder.getAttach());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTimeExpire(payOrder.getExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        /*
         * 花呗分期相关的设置,测试环境不支持花呗分期的测试
         * hb_fq_num代表花呗分期数，仅支持传入3、6、12，其他期数暂不支持，传入会报错；
         * hb_fq_seller_percent代表卖家承担收费比例，商家承担手续费传入100，用户承担手续费传入0，仅支持传入100、0两种，其他比例暂不支持，传入会报错。
         */
//            ExtendParams extendParams = new ExtendParams();
//            extendParams.setHbFqNum("3");
//            extendParams.setHbFqSellerPercent("0");
//            model.setExtendParams(extendParams);
        try {
            HttpServletResponse response = ServletUtils.getResponse();
            AliPayApi.tradePage(response, model, config.getNotifyUrl(), config.getReturnUrl());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Strix Pay: 支付宝支付下单失败. (支付宝支付下单异常)", e);
        }
        return null;
    }

    @Override
    public boolean verifyNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = AliPayApi.toMap(request);
            return AlipaySignature.rsaCertCheckV1(params, config.getAliPayCertPath(), "UTF-8", "RSA2");
        } catch (Exception e) {
            log.error("Strix Pay: 支付宝支付回调验证失败.", e);
            return false;
        }
    }

    @Override
    public BasePayResult resolveResult(HttpServletRequest request) {
        Map<String, String> params = AliPayApi.toMap(request);
//        for (Map.Entry<String, String> entry : params.entrySet()) {
//            log.info(entry.getKey() + " = " + entry.getValue());
//        }

        BasePayResult result = new BasePayResult();
        result.setSuccess("TRADE_SUCCESS".equals(MapUtil.getStr(params, "trade_status")));
        result.setOrderId(MapUtil.getStr(params, "out_trade_no"));
        result.setPlatformOrderNo(MapUtil.getStr(params, "trade_no"));
        result.setTotalAmount((int) (MapUtil.getDouble(params, "total_amount") * 100));
        result.setPlatformUserId(MapUtil.getStr(params, "buyer_id"));
        result.setAttach(MapUtil.getStr(params, "passback_params"));
        try {
            result.setOriginalResult(objectMapper.writeValueAsString(params));
        } catch (Exception e) {
            log.warn("Strix Pay: 支付宝支付回调结果序列化失败.", e);
        }
        return result;
    }

    @Override
    public void respondNotify(boolean success, HttpServletResponse response) {
        ServletUtils.write(response, success ? "success" : "failure");
    }

}
