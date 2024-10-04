package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/11/29 16:34
 */
@Component
@Dict(key = "WorkflowStepRouteType", value = "工作流-步骤路径-类型")
public class WorkflowStepRouteType implements BaseDict {

    @DictData(label = "成功", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    byte SUCCESS = 1;

    @DictData(label = "失败", sort = 2, style = DictDataStyle.ERROR)
    public static final
    byte FAIL = 2;

    @DictData(label = "取消", sort = 3, style = DictDataStyle.WARNING)
    public static final
    byte END = 3;

    @DictData(label = "定时器", sort = 4, style = DictDataStyle.INFO)
    public static final
    byte TIMER = 4;

    public static boolean valid(Byte value) {
        return value > 0;
    }

}
