package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2021/8/26 15:17
 */
@Dict(key = "SystemUserStatus", value = "系统用户-状态")
public class SystemUserStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short BANNED = 2;

    public static boolean valid(short value) {
        return value == BANNED || value == NORMAL;
    }

}
