package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @since 2024-10-15 14:53:53
 */
@Component
@Dict(key = "WorkflowPropsTimeLimitUnit", value = "工作流-配置-超时时间单位")
public class WorkflowPropsTimeLimitUnit implements BaseDict {

    @DictData(label = "分钟", sort = 1, style = DictDataStyle.DEFAULT)
    public static final String MINUTE = "MINUTE";

    @DictData(label = "小时", sort = 2, style = DictDataStyle.DEFAULT)
    public static final String HOUR = "HOUR";

    @DictData(label = "天", sort = 3, style = DictDataStyle.DEFAULT)
    public static final String DAY = "DAY";

    public static boolean valid(String value) {
        return MINUTE.equals(value) || HOUR.equals(value) || DAY.equals(value);
    }

}
