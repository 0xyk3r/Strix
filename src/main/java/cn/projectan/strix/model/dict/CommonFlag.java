package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/11/29 16:08
 */
@Dict(key = "CommonSwitch", value = "通用是否标识")
public interface CommonFlag {

    @DictData(label = "否", sort = 0, style = DictDataStyle.ERROR)
    int NO = 0;

    @DictData(label = "是", sort = 1, style = DictDataStyle.SUCCESS)
    int YES = 1;

    static boolean valid(int value) {
        return value == NO || value == YES;
    }

}
