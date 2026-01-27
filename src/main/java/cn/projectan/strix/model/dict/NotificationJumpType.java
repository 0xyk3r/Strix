package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * 通知跳转类型
 *
 * @author ProjectAn
 * @since 2026/1/13 16:30
 */
@Dict(key = "NotificationJumpType", value = "通知-跳转类型")
public class NotificationJumpType implements BaseDict {

    @DictData(label = "页面跳转", sort = 1, style = DictDataStyle.INFO)
    public static final
    String PAGE = "PAGE";

    @DictData(label = "URL 跳转", sort = 2, style = DictDataStyle.INFO)
    public static final
    String URL = "URL";

    @DictData(label = "无跳转", sort = 3, style = DictDataStyle.DEFAULT)
    public static final
    String NONE = "NONE";

    public static boolean valid(String jumpType) {
        return PAGE.equals(jumpType) || URL.equals(jumpType) || NONE.equals(jumpType);
    }

}
