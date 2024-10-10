package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-10 14:00:43
 */
@Component
@Dict(key = "WorkflowPropsAssignMode", value = "工作流-配置-指派人员模式")
public class WorkflowPropsAssignMode implements BaseDict {

    @DictData(label = "任一", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String ANY = "ANY";

    @DictData(label = "同时", sort = 2, style = DictDataStyle.DEFAULT)
    public static final
    String ALL = "ALL";

    @DictData(label = "顺序", sort = 3, style = DictDataStyle.DEFAULT)
    public static final
    String SEQ = "SEQ";

    public static boolean valid(String value) {
        return ANY.equals(value) || ALL.equals(value) || SEQ.equals(value);
    }
}
