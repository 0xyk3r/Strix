package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/5/28 22:15
 */
@Component
@Dict(key = "DictStatus", value = "系统字典-状态")
public class DictStatus implements BaseDict {

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
