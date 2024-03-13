package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/8/1 14:39
 */
@Dict(key = "CommonSwitch", value = "通用简易开关")
public interface CommonSwitch {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    int ENABLE = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    int DISABLE = 2;

    static boolean valid(int value) {
        return value == ENABLE || value == DISABLE;
    }

}
