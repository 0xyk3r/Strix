package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.other.module.pay.BasePayParam;
import cn.projectan.strix.model.other.module.pay.BasePayResult;
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
     * @see cn.projectan.strix.model.dict.PayType 支付类型
     */
    Map<String, String> createOrder(String configId, String title, BasePayParam param, String attach, Integer amount, Integer expireMin, String handlerId, Integer payType);

    /**
     * 处理支付结果
     *
     * @param payResult 支付结果
     */
    void handlePayResult(BasePayResult payResult);

    void handleExpired(String orderId);

}
