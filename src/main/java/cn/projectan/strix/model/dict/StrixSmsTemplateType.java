package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/5/20 18:46
 */
@Component
@Dict(key = "StrixSmsTemplateType", value = "短信服务-短信模板-类型")
public class StrixSmsTemplateType implements BaseDict {

    @DictData(label = "验证码", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int VERIFICATION_CODE = 1;

    @DictData(label = "通知短信", sort = 2, style = DictDataStyle.PRIMARY)
    public static final
    int NOTIFICATION = 2;

    @DictData(label = "营销短信", sort = 3, style = DictDataStyle.WARNING)
    public static final
    int MARKETING = 3;

    @DictData(label = "国际短信", sort = 4, style = DictDataStyle.ERROR)
    public static final
    int INTERNATIONAL = 4;

    @DictData(label = "数字短信", sort = 5, style = DictDataStyle.INFO)
    public static final
    int DIGITAL = 5;

}
