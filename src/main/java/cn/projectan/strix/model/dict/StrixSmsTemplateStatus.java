package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/5/20 17:35
 */
@Component
@Dict(key = "StrixSmsTemplateStatus", value = "短信服务-短信模板-状态")
public class StrixSmsTemplateStatus implements BaseDict {

    @DictData(label = "待审核", sort = 1, style = DictDataStyle.WARNING)
    public static final
    int INIT = 1;

    @DictData(label = "审核通过", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    int PASS = 2;

    @DictData(label = "审核失败", sort = 3, style = DictDataStyle.ERROR)
    public static final
    int NOT_PASS = 3;

    @DictData(label = "审核取消", sort = 4, style = DictDataStyle.INFO)
    public static final
    int CANCEL = 4;

}
