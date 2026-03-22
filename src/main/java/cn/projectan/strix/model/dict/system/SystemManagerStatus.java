package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统管理用户 状态
 *
 * @author ProjectAn
 * @since 2021/5/12 18:52
 */
@Dict(key = "SystemManagerStatus", value = "系统人员-状态")
@Schema(description = "系统人员-状态")
public class SystemManagerStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    public static final
    short BANNED = 2;

    public static boolean valid(short value) {
        return value == NORMAL || value == BANNED;
    }

}
