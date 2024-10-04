package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/5/30 11:08
 */
@Component
@Dict(key = "DictProvided", value = "系统字典-是否内置")
public class DictProvided implements BaseDict {

    @DictData(label = "是", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int YES = 1;

    @DictData(label = "否", sort = 2, style = DictDataStyle.INFO)
    public static final
    int NO = 2;

}
