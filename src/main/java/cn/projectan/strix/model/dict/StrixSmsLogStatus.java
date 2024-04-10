package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/5/22 14:22
 */
@Component
@Dict(key = "StrixSmsLogStatus", value = "短信服务-短信日志-状态")
public class StrixSmsLogStatus implements BaseDict {

    @DictData(label = "待发送", sort = 1, style = DictDataStyle.WARNING)
    public static final
    int INIT = 1;

    @DictData(label = "发送成功", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    int SUCCESS = 2;

    @DictData(label = "发送失败", sort = 3, style = DictDataStyle.ERROR)
    public static final
    int FAIL = 3;

}
