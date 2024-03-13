package cn.projectan.strix.model.dict;

/**
 * @author ProjectAn
 * @date 2021/9/3 11:02
 */
public interface PaymentOrderStatus {

    /**
     * 未支付
     */
    int UNPAID = 1;

    /**
     * 已支付
     */
    int PAID = 2;

    /**
     * 已退款
     */
    int REFUNDED = 3;

}
