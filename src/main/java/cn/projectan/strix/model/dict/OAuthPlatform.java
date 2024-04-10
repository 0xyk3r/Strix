package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2024/4/3 17:04
 */
@Component
@Dict(key = "OAuthPlatform", value = "OAuth平台")
public class OAuthPlatform implements BaseDict {

    @DictData(label = "微信", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int WECHAT = 1;

    @DictData(label = "支付宝", sort = 2, style = DictDataStyle.INFO)
    public static final
    int ALIPAY = 2;

    public static boolean valid(int value) {
        return value == WECHAT || value == ALIPAY;
    }

}
