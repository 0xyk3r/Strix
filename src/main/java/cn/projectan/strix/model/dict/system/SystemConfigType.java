package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2026/1/29 07:23
 */
@Dict(key = "SystemConfigType", value = "系统开关-类型")
@Schema(description = "系统开关-类型")
public class SystemConfigType implements BaseDict {

    @DictData(label = "开关", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    short SWITCH = 1;

    @DictData(label = "内容", sort = 2, style = DictDataStyle.INFO)
    public static final
    short CONTENT = 2;

    public static boolean valid(short value) {
        return value == SWITCH || value == CONTENT;
    }

}
