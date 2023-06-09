package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/5/20 15:15
 */
@Dict(key = "StrixSmsPlatform", value = "短信服务-平台")
public interface StrixSmsPlatform {

    @DictData(label = "阿里云", sort = 1, style = DictDataStyle.WARNING)
    int ALIYUN = 1;

    @DictData(label = "腾讯云", sort = 2, style = DictDataStyle.INFO)
    int TENCENT = 2;

    static boolean valid(int platform) {
        return platform == ALIYUN || platform == TENCENT;
    }

}
