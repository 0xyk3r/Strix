package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/11/29 16:34
 */
@Dict(key = "WorkflowStepType", value = "工作流-步骤路径-类型")
public interface WorkflowStepRouteType {

    @DictData(label = "成功", sort = 1, style = DictDataStyle.SUCCESS)
    byte SUCCESS = 1;

    @DictData(label = "失败", sort = 2, style = DictDataStyle.ERROR)
    byte FAIL = 2;

    @DictData(label = "取消", sort = 3, style = DictDataStyle.WARNING)
    byte END = 3;

    @DictData(label = "定时器", sort = 4, style = DictDataStyle.INFO)
    byte TIMER = 4;

    static boolean valid(Byte value) {
        return value > 0;
    }

}
