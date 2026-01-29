package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * 支付订单状态
 *
 * @author ProjectAn
 * @since 2021/9/3 11:02
 */
@Dict(key = "PayOrderStatus", value = "支付订单状态")
public class PayOrderStatus implements BaseDict {

    @DictData(label = "未支付", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    short UNPAID = 1;

    @DictData(label = "已支付", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short PAID = 2;

    @DictData(label = "已退款", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short REFUNDED = 3;

    @DictData(label = "超时未支付", sort = 99, style = DictDataStyle.WARNING)
    public static final
    short EXPIRED = 99;

    public static boolean valid(short value) {
        return value == UNPAID || value == PAID || value == REFUNDED || value == EXPIRED;
    }

}
