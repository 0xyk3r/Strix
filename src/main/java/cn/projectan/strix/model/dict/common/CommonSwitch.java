package cn.projectan.strix.model.dict.common;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import cn.projectan.strix.model.dict.system.DictDataStyle;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2023/8/1 14:39
 */

@Dict(key = "CommonSwitch", value = "通用开关标识")
@Schema(description = "通用开关标识")
public class CommonSwitch implements BaseDict {

    @DictData(label = "禁用", sort = 0, style = DictDataStyle.ERROR)
    public static final
    short DISABLE = 0;

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short ENABLE = 1;

    public static boolean valid(short value) {
        return value == DISABLE || value == ENABLE;
    }

}
