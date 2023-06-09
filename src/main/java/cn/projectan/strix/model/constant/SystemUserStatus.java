package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2021/8/26 15:17
 */
@Dict(key = "SystemUserStatus", value = "系统用户-状态")
public interface SystemUserStatus {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    int NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.SUCCESS)
    int BANNED = 2;

    static boolean valid(int status) {
        return status == BANNED || status == NORMAL;
    }

}
