package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-07 06:53:26
 */
@Component
@Dict(key = "WorkflowNodeType", value = "工作流-节点-类型")
public class WorkflowNodeType implements BaseDict {

    @DictData(label = "起始", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String ROOT = "root";

    @DictData(label = "审批", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    String APPROVAL = "approval";

    @DictData(label = "办理", sort = 3, style = DictDataStyle.WARNING)
    public static final
    String TASK = "task";

    @DictData(label = "抄送", sort = 4, style = DictDataStyle.PRIMARY)
    public static final
    String CC = "cc";

    @DictData(label = "条件", sort = 5, style = DictDataStyle.INFO)
    public static final
    String CONDITION = "condition";

    @DictData(label = "条件组", sort = 6, style = DictDataStyle.INFO)
    public static final
    String CONDITIONS = "conditions";

    public static boolean valid(String value) {
        return ROOT.equals(value) || APPROVAL.equals(value) || TASK.equals(value) || CC.equals(value) || CONDITION.equals(value) || CONDITIONS.equals(value);
    }

}
