package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-15 14:54:09
 */
@Component
@Dict(key = "WorkflowPropsTimeLimitHandler", value = "工作流-配置-超时后操作")
public class WorkflowPropsTimeLimitHandler implements BaseDict {

    @DictData(label = "发送通知", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String NOTIFY = "NOTIFY";

    @DictData(label = "自动通过", sort = 2, style = DictDataStyle.DEFAULT)
    public static final
    String AUTO_PASS = "AUTO_PASS";

    @DictData(label = "自动拒绝", sort = 3, style = DictDataStyle.DEFAULT)
    public static final
    String AUTO_REJECT = "AUTO_REJECT";

    public static boolean valid(String value) {
        return NOTIFY.equals(value) || AUTO_PASS.equals(value) || AUTO_REJECT.equals(value);
    }

}
