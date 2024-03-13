package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/11/29 16:02
 */
@Dict(key = "WorkflowStepType", value = "工作流-步骤-类型")
public interface WorkflowStepType {

    @DictData(label = "普通", sort = 1, style = DictDataStyle.INFO)
    byte NORMAL = 1;

    @DictData(label = "起点", sort = 2, style = DictDataStyle.SUCCESS)
    byte START = 2;

    @DictData(label = "终点", sort = 3, style = DictDataStyle.WARNING)
    byte END = 3;

    static boolean valid(Byte value) {
        return value != null && (value.equals(NORMAL) || value.equals(START) || value.equals(END));
    }

}
