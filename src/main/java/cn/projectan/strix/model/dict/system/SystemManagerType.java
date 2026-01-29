package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * 系统管理用户 类型
 *
 * @author ProjectAn
 * @since 2021/6/16 15:32
 */

@Dict(key = "SystemManagerType", value = "系统人员-类型")
public class SystemManagerType implements BaseDict {

    @DictData(label = "超级账号", sort = 99, style = DictDataStyle.SUCCESS)
    public static final
    short SUPER_ACCOUNT = 1;

    @DictData(label = "普通账号", sort = 1, style = DictDataStyle.PRIMARY)
    public static final
    short NORMAL_ACCOUNT = 2;

    public static boolean valid(short value) {
        return value == SUPER_ACCOUNT || value == NORMAL_ACCOUNT;
    }

}
