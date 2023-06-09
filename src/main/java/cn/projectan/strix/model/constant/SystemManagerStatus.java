package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * 系统管理用户 状态
 *
 * @author 安炯奕
 * @date 2021/5/12 18:52
 */
@Dict(key = "SystemManagerStatus", value = "系统人员-状态")
public interface SystemManagerStatus {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    int NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    int BANNED = 2;

    static boolean valid(Integer status) {
        return status != null && (status == BANNED || status == NORMAL);
    }

}
