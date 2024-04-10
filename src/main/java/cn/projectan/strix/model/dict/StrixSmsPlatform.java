package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/5/20 15:15
 */
@Component
@Dict(key = "StrixSmsPlatform", value = "短信服务-平台")
public class StrixSmsPlatform implements BaseDict {

    @DictData(label = "阿里云", sort = 1, style = DictDataStyle.WARNING)
    public static final
    int ALIYUN = 1;

    @DictData(label = "腾讯云", sort = 2, style = DictDataStyle.INFO)
    public static final
    int TENCENT = 2;

    public static boolean valid(int platform) {
        return platform == ALIYUN || platform == TENCENT;
    }

}
