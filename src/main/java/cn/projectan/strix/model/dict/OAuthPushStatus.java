package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import org.springframework.stereotype.Component;

/**
 * OAuth推送状态
 *
 * @author ProjectAn
 * @date 2024/4/8 下午5:37
 */
@Component
@Dict(key = "OAuthPushStatus", value = "OAuth推送状态")
public class OAuthPushStatus {

    @DictData(label = "等待", sort = 1, style = DictDataStyle.DEFAULT)
    public static final byte WAITING = 1;

    @DictData(label = "成功", sort = 2, style = DictDataStyle.SUCCESS)
    public static final byte SUCCESS = 2;

    @DictData(label = "失败", sort = 3, style = DictDataStyle.ERROR)
    public static final byte FAILURE = 3;

    public static boolean valid(byte value) {
        return value == WAITING || value == SUCCESS || value == FAILURE;
    }

}
