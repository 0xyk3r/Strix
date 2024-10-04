package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * 支付订单状态
 *
 * @author ProjectAn
 * @since 2021/9/3 11:02
 */
@Component
@Dict(key = "PayOrderStatus", value = "支付订单状态")
public class PayOrderStatus implements BaseDict {

    @DictData(label = "未支付", sort = 1, style = DictDataStyle.DEFAULT)
    public static final int UNPAID = 1;

    @DictData(label = "已支付", sort = 2, style = DictDataStyle.SUCCESS)
    public static final int PAID = 2;

    @DictData(label = "已退款", sort = 3, style = DictDataStyle.ERROR)
    public static final int REFUNDED = 3;

    @DictData(label = "超时未支付", sort = 99, style = DictDataStyle.WARNING)
    public static final int EXPIRED = 99;

    public static boolean valid(byte value) {
        return value == UNPAID || value == PAID || value == REFUNDED || value == EXPIRED;
    }

}
