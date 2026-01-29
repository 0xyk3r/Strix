package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2023/5/20 15:15
 */
@Dict(key = "SmsPlatform", value = "短信服务-平台")
public class SmsPlatform implements BaseDict {

    @DictData(label = "阿里云", sort = 1, style = DictDataStyle.WARNING)
    public static final
    short ALIYUN = 1;

    @DictData(label = "腾讯云", sort = 2, style = DictDataStyle.INFO)
    public static final
    short TENCENT = 2;

    public static boolean valid(short value) {
        return value == ALIYUN || value == TENCENT;
    }

}
