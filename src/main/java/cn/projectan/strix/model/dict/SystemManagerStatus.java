package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * 系统管理用户 状态
 *
 * @author ProjectAn
 * @date 2021/5/12 18:52
 */
@Component
@Dict(key = "SystemManagerStatus", value = "系统人员-状态")
public class SystemManagerStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.ERROR)
    public static final
    int BANNED = 2;

    public static boolean valid(Integer status) {
        return status != null && (status == BANNED || status == NORMAL);
    }

}
