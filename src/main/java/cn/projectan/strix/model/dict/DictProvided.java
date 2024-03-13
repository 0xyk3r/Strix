package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/5/30 11:08
 */
@Dict(key = "DictProvided", value = "系统字典-是否内置")
public interface DictProvided {

    @DictData(label = "是", sort = 1, style = DictDataStyle.SUCCESS)
    int YES = 1;

    @DictData(label = "否", sort = 2, style = DictDataStyle.INFO)
    int NO = 2;

}
