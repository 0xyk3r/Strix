package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2023/5/20 17:35
 */
@Dict(key = "SmsTemplateStatus", value = "短信服务-短信模板-状态")
public class SmsTemplateStatus implements BaseDict {

    @DictData(label = "待审核", sort = 1, style = DictDataStyle.WARNING)
    public static final
    short INIT = 1;

    @DictData(label = "审核通过", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short PASS = 2;

    @DictData(label = "审核失败", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short NOT_PASS = 3;

    @DictData(label = "审核取消", sort = 4, style = DictDataStyle.INFO)
    public static final
    short CANCEL = 4;

}
