package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/11/29 16:08
 */
@Component
@Dict(key = "PayType", value = "支付方式")
public class PayType implements BaseDict {

    @DictData(label = "移动端网页", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int WAP = 1;

    @DictData(label = "PC端网页", sort = 2, style = DictDataStyle.INFO)
    public static final
    int WEB = 2;

    @DictData(label = "APP端", sort = 3, style = DictDataStyle.ERROR)
    public static final
    int APP = 3;

    public static boolean valid(int value) {
        return value == WAP || value == WEB || value == APP;
    }

}
