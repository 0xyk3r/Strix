package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.other.module.pay.PaymentData;
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
public interface PayOrderService extends IService<PayOrder> {

    /**
     * 生成 JsApi 支付请求参数
     *
     * @param configId 支付配置id
     * @param title    支付内容标题
     * @param data     支付参数json 微信支付时需包含付款人openId
     * @param attach   支付回调参数json
     * @param amount   支付总金额
     * @param payType  支付类型
     * @return JSAPI支付参数
     */
    Map<String, String> createOrder(String configId, String title, PaymentData data, String attach, Integer amount, Integer payType);

    /**
     * 保存支付结果
     *
     * @param payResult 支付结果
     * @throws Exception 异常
     */
    void savePayResult(Map<String, Object> payResult) throws Exception;

}
