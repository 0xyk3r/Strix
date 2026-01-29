package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * OAuth推送状态
 *
 * @author ProjectAn
 * @since 2024/4/8 下午5:37
 */
@Dict(key = "OAuthPushStatus", value = "OAuth推送状态")
public class OAuthPushStatus implements BaseDict {

    @DictData(label = "等待", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    short WAITING = 1;

    @DictData(label = "成功", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short SUCCESS = 2;

    @DictData(label = "失败", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short FAILURE = 3;

    public static boolean valid(short value) {
        return value == WAITING || value == SUCCESS || value == FAILURE;
    }

}
