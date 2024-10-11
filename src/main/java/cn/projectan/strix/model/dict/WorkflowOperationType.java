package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-09 11:22:42
 */
@Component
@Dict(key = "WorkflowOperationType", value = "工作流-操作-类型")
public class WorkflowOperationType implements BaseDict {

    @DictData(label = "发起", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    byte INITIATE = 1;

    @DictData(label = "通过", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    byte APPROVED = 2;

    @DictData(label = "拒绝", sort = 3, style = DictDataStyle.ERROR)
    public static final
    byte REJECT = 3;

    @DictData(label = "取消", sort = 4, style = DictDataStyle.WARNING)
    public static final
    byte CANCEL = 4;

    @DictData(label = "退回", sort = 5, style = DictDataStyle.DEFAULT)
    public static final
    byte BACK = 5;

    @DictData(label = "指派", sort = 6, style = DictDataStyle.DEFAULT)
    public static final
    byte REASSIGN = 6;

    @DictData(label = "自动", sort = 7, style = DictDataStyle.DEFAULT)
    public static final
    byte AUTO = 7;

    @DictData(label = "抄送", sort = 8, style = DictDataStyle.DEFAULT)
    public static final
    byte CC = 8;

    public static boolean valid(Byte value) {
        return INITIATE == value || APPROVED == value || REJECT == value || CANCEL == value || BACK == value || REASSIGN == value || AUTO == value || CC == value;
    }

}
