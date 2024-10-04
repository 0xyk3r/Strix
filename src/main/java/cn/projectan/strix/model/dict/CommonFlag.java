package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/11/29 16:08
 */
@Component
@Dict(key = "CommonFlag", value = "通用是否标识")
public class CommonFlag implements BaseDict {

    @DictData(label = "否", sort = 0, style = DictDataStyle.ERROR)
    public static final
    int NO = 0;

    @DictData(label = "是", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int YES = 1;

    public static boolean valid(int value) {
        return value == NO || value == YES;
    }

}
