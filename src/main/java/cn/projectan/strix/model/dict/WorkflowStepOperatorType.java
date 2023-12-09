package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/11/29 16:39
 */
@Dict(key = "WorkflowStepType", value = "工作流-步骤路径-操作人类型")
public interface WorkflowStepOperatorType {

    @DictData(label = "系统管理人员", sort = 1, style = DictDataStyle.DEFAULT)
    byte MANAGER = 1;

    @DictData(label = "系统用户", sort = 2, style = DictDataStyle.INFO)
    byte USER = 2;

    static boolean valid(Byte value) {
        return value != null && (value.equals(MANAGER) || value.equals(USER));
    }

}
