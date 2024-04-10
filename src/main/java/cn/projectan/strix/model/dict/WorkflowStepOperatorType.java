package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/11/29 16:39
 */
@Component
@Dict(key = "WorkflowStepOperatorType", value = "工作流-步骤路径-操作人类型")
public class WorkflowStepOperatorType implements BaseDict {

    @DictData(label = "系统管理人员", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    byte MANAGER = 1;

    @DictData(label = "系统用户", sort = 2, style = DictDataStyle.INFO)
    public static final
    byte USER = 2;

    public static boolean valid(Byte value) {
        return value != null && (value.equals(MANAGER) || value.equals(USER));
    }

}
