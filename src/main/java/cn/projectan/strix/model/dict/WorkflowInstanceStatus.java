package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/11/29 15:58
 */
@Dict(key = "WorkflowInstanceStatus", value = "工作流-实例-状态")
public interface WorkflowInstanceStatus {

    @DictData(label = "进行中", sort = 1, style = DictDataStyle.DEFAULT)
    byte ACTIVE = 1;

    @DictData(label = "已完成", sort = 2, style = DictDataStyle.SUCCESS)
    byte DONE = 2;

    @DictData(label = "已取消", sort = 3, style = DictDataStyle.INFO)
    byte CANCEL = 3;

    static boolean valid(Byte value) {
        return value != null && (value.equals(ACTIVE) || value.equals(DONE) || value.equals(CANCEL));
    }

}
