package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2023/5/22 14:22
 */
@Dict(key = "SmsLogStatus", value = "短信服务-短信日志-状态")
public class SmsLogStatus implements BaseDict {

    @DictData(label = "待发送", sort = 1, style = DictDataStyle.WARNING)
    public static final
    short INIT = 1;

    @DictData(label = "发送成功", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short SUCCESS = 2;

    @DictData(label = "发送失败", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short FAIL = 3;

}
