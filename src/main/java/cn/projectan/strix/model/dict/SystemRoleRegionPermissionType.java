package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * 系统管理用户 类型
 *
 * @author ProjectAn
 * @since 2021/6/16 15:32
 */
@Component
@Dict(key = "SystemRoleRegionPermissionType", value = "系统角色-地区权限类型")
public class SystemRoleRegionPermissionType implements BaseDict {

    @DictData(label = "全部数据权限", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    byte ALL_REGION = 1;

    @DictData(label = "所属地区及子地区", sort = 2, style = DictDataStyle.PRIMARY)
    public static final
    byte WITH_SUB_REGION = 2;

    @DictData(label = "仅所属地区", sort = 3, style = DictDataStyle.WARNING)
    public static final
    byte CURR_REGION = 3;

//    @DictData(label = "自定义", sort = 4, style = DictDataStyle.ERROR)
//    public static final
//    byte CUSTOM = 4;

    public static boolean valid(int type) {
        return type == ALL_REGION || type == WITH_SUB_REGION || type == CURR_REGION;
    }

}
