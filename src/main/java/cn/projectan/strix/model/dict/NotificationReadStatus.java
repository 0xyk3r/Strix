package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * 通知已读状态
 *
 * @author ProjectAn
 * @since 2026/1/13 16:30
 */
@Dict(key = "NotificationReadStatus", value = "通知-已读状态")
public class NotificationReadStatus implements BaseDict {

    @DictData(label = "未读", sort = 1, style = DictDataStyle.WARNING)
    public static final
    short UNREAD = 0;

    @DictData(label = "已读", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short READ = 1;

    public static boolean valid(Short readStatus) {
        return readStatus != null && (readStatus == UNREAD || readStatus == READ);
    }

}
