package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/5/30 12:23
 */
@Dict(key = "DictDataStatus", value = "系统字典-数据-状态")
public interface DictDataStatus {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    int ENABLE = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    int DISABLE = 2;

    static boolean valid(int value) {
        return value == ENABLE || value == DISABLE;
    }

}
