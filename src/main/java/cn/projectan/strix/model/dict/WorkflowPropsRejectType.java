package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-10 14:24:09
 */
@Component
@Dict(key = "WorkflowPropsRejectType", value = "工作流-配置-审批拒绝后操作")
public class WorkflowPropsRejectType implements BaseDict {

    @DictData(label = "结束流程", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String END = "END";

    @DictData(label = "返回上一节点", sort = 2, style = DictDataStyle.DEFAULT)
    public static final
    String BACK = "BACK";

    @DictData(label = "返回指定节点", sort = 3, style = DictDataStyle.DEFAULT)
    public static final
    String NODE = "NODE";

    public static boolean valid(String value) {
        return END.equals(value) || BACK.equals(value) || NODE.equals(value);
    }

}
