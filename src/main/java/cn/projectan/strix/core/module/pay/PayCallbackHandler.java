package cn.projectan.strix.core.module.pay;

/**
 * 支付回调处理器接口
 * <p>
 * 所有支付回调处理器必须实现此接口并注册为 Spring Bean。
 * 数据库 sys_pay_handler 表的 handler 字段存储 Bean 名称。
 * </p>
 *
 * @author ProjectAn
 * @since 2025-03-21
 */
public interface PayCallbackHandler {

    /**
     * 支付成功回调
     *
     * @param orderId 订单ID
     */
    void onPaySuccess(String orderId);

    /**
     * 退款回调
     *
     * @param orderId 订单ID
     */
    void onPayRefund(String orderId);

    /**
     * 支付超时回调
     *
     * @param orderId 订单ID
     */
    void onPayTimeout(String orderId);

}
