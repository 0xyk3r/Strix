package cn.projectan.strix.model.dict.common;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import cn.projectan.strix.model.dict.system.DictDataStyle;

/**
 * @author ProjectAn
 * @since 2023/8/1 14:39
 */

@Dict(key = "CommonSwitch", value = "通用简易开关")
public class CommonSwitch implements BaseDict {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short ENABLE = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    public static final
    short DISABLE = 2;

    public static boolean valid(short value) {
        return value == ENABLE || value == DISABLE;
    }

}
