package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2023/11/29 15:58
 */
@Component
@Dict(key = "WorkflowInstanceStatus", value = "工作流-实例-状态")
public class WorkflowInstanceStatus implements BaseDict {

    @DictData(label = "进行中", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    byte ACTIVE = 1;

    @DictData(label = "已完成", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    byte DONE = 2;

    @DictData(label = "已取消", sort = 3, style = DictDataStyle.INFO)
    public static final
    byte CANCEL = 3;

    public static boolean valid(Byte value) {
        return value != null && (value.equals(ACTIVE) || value.equals(DONE) || value.equals(CANCEL));
    }

}
