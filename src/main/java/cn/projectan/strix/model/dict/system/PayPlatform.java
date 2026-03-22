package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2023/11/29 16:08
 */
@Dict(key = "PayPlatform", value = "支付平台")
@Schema(description = "支付平台")
public class PayPlatform implements BaseDict {

    @DictData(label = "微信支付", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short WX_PAY = 1;

    @DictData(label = "支付宝", sort = 2, style = DictDataStyle.INFO)
    public static final
    short ALI_PAY = 2;

    public static boolean valid(short value) {
        return value == WX_PAY || value == ALI_PAY;
    }

}
