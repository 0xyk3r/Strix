package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-09 13:34:31
 */
@Component
@Dict(key = "WorkflowPropsAssignType", value = "工作流-配置-指派人员类型")
public class WorkflowPropsAssignType implements BaseDict {

    @DictData(label = "指定人员", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String USER = "USER";

    @DictData(label = "指定角色", sort = 2, style = DictDataStyle.DEFAULT)
    public static final
    String ROLE = "ROLE";

    @DictData(label = "发起人自选", sort = 3, style = DictDataStyle.DEFAULT)
    public static final
    String SELECT = "SELECT";

    @DictData(label = "发起人自己", sort = 4, style = DictDataStyle.DEFAULT)
    public static final
    String SELF = "SELF";

    @DictData(label = "系统自动拒绝", sort = 5, style = DictDataStyle.DEFAULT)
    public static final
    String AUTO_REJECT = "AUTO_REJECT";

    public static boolean valid(String value) {
        return USER.equals(value) || ROLE.equals(value) || SELECT.equals(value) || SELF.equals(value) || AUTO_REJECT.equals(value);
    }

}
