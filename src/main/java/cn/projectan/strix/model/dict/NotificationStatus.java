package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * 通知状态
 *
 * @author ProjectAn
 * @since 2026/1/13 16:30
 */
@Dict(key = "NotificationStatus", value = "通知-状态")
public class NotificationStatus implements BaseDict {

    @DictData(label = "有效", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short VALID = 1;

    @DictData(label = "已终止", sort = 2, style = DictDataStyle.ERROR)
    public static final
    short TERMINATED = 2;

    public static boolean valid(Short status) {
        return status != null && (status == VALID || status == TERMINATED);
    }

}
