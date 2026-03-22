package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2024/4/3 17:04
 */
@Dict(key = "OAuthPlatform", value = "OAuth平台")
@Schema(description = "OAuth平台")
public class OAuthPlatform implements BaseDict {

    @DictData(label = "微信公众号", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short WECHAT_OA = 1;

    @DictData(label = "微信小程序", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short WECHAT_MP = 2;

    @DictData(label = "支付宝", sort = 11, style = DictDataStyle.INFO)
    public static final
    short ALIPAY = 11;

    public static boolean valid(short value) {
        return value == WECHAT_OA || value == WECHAT_MP || value == ALIPAY;
    }

}
