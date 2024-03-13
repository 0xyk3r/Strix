package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/5/22 14:22
 */
@Dict(key = "StrixSmsLogStatus", value = "短信服务-短信日志-状态")
public interface StrixSmsLogStatus {

    @DictData(label = "待发送", sort = 1, style = DictDataStyle.WARNING)
    int INIT = 1;

    @DictData(label = "发送成功", sort = 2, style = DictDataStyle.SUCCESS)
    int SUCCESS = 2;

    @DictData(label = "发送失败", sort = 3, style = DictDataStyle.ERROR)
    int FAIL = 3;

}
