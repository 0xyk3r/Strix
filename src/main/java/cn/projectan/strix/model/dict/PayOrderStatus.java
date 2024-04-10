package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @date 2021/9/3 11:02
 */
public class PayOrderStatus implements BaseDict {

    /**
     * 未支付
     */
    public static final int UNPAID = 1;

    /**
     * 已支付
     */
    public static final int PAID = 2;

    /**
     * 已退款
     */
    public static final int REFUNDED = 3;

}
