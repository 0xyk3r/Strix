package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/8/1 14:39
 */
@Component
@Dict(key = "CommonSwitch", value = "通用简易开关")
public class CommonSwitch implements BaseDict {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int ENABLE = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    public static final
    int DISABLE = 2;

    public static boolean valid(int value) {
        return value == ENABLE || value == DISABLE;
    }

}
