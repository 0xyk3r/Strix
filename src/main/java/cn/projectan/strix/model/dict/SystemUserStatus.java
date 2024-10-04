package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2021/8/26 15:17
 */
@Component
@Dict(key = "SystemUserStatus", value = "系统用户-状态")
public class SystemUserStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int NORMAL = 1;

    @DictData(label = "禁用", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    int BANNED = 2;

    public static boolean valid(int status) {
        return status == BANNED || status == NORMAL;
    }

}
