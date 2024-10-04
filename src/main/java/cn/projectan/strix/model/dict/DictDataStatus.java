package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/5/30 12:23
 */
@Component
@Dict(key = "DictDataStatus", value = "系统字典-数据-状态")
public class DictDataStatus implements BaseDict {

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
