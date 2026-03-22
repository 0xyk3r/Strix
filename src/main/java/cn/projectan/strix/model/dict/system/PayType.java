package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2023/11/29 16:08
 */
@Dict(key = "PayType", value = "支付方式")
@Schema(description = "支付方式")
public class PayType implements BaseDict {

    @DictData(label = "移动端网页", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short WAP = 1;

    @DictData(label = "PC端网页", sort = 2, style = DictDataStyle.INFO)
    public static final
    short WEB = 2;

    @DictData(label = "APP端", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short APP = 3;

    public static boolean valid(short value) {
        return value == WAP || value == WEB || value == APP;
    }

}
