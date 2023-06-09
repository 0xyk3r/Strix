package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/5/28 22:15
 */
@Dict(key = "DictStatus", value = "系统字典-状态")
public interface DictStatus {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    int ENABLE = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    int DISABLE = 2;

    static boolean valid(int value) {
        return value == ENABLE || value == DISABLE;
    }

}
