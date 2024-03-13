package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PaymentOrder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 生成微信JsApi支付请求参数
     *
     * @param paymentConfigId 支付配置id
     * @param title           支付内容标题
     * @param paymentData     支付参数json 微信支付时需包含付款人openId
     * @param attach          支付回调参数json
     * @param totalAmount     支付总金额
     * @return JSAPI支付参数
     */
    Map<String, String> genWxPayJsApiOrder(String paymentConfigId, String title, String paymentData, String attach, Integer totalAmount);

    /**
     * 保存支付结果
     *
     * @param payResult 支付结果
     * @throws Exception 异常
     */
    void savePayResult(Map<String, Object> payResult) throws Exception;

}
