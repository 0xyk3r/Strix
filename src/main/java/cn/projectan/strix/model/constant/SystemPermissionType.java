package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * 系统权限 权限类型
 *
 * @author 安炯奕
 * @date 2021/7/20 15:48
 */
@Dict(key = "SystemPermissionType", value = "系统权限-权限类型")
public interface SystemPermissionType {

    @DictData(label = "只读权限", sort = 1, style = DictDataStyle.PRIMARY)
    int READ_ONLY = 1;

    @DictData(label = "读写权限", sort = 2, style = DictDataStyle.SUCCESS)
    int READ_WRITE = 2;

    static boolean valid(int type) {
        return type == READ_ONLY || type == READ_WRITE;
    }

}
