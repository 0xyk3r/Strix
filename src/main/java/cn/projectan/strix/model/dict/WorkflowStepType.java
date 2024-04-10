package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/11/29 16:02
 */
@Component
@Dict(key = "WorkflowStepType", value = "工作流-步骤-类型")
public class WorkflowStepType implements BaseDict {

    @DictData(label = "普通", sort = 1, style = DictDataStyle.INFO)
    public static final
    byte NORMAL = 1;

    @DictData(label = "起点", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    byte START = 2;

    @DictData(label = "终点", sort = 3, style = DictDataStyle.WARNING)
    public static final
    byte END = 3;

    public static boolean valid(Byte value) {
        return value != null && (value.equals(NORMAL) || value.equals(START) || value.equals(END));
    }

}
