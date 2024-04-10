package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/8/1 14:33
 */
@Component
@Dict(key = "JobMisfire", value = "系统定时任务-错过执行策略")
public class JobMisfire implements BaseDict {

    @DictData(label = "默认策略", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int DEFAULT = 1;

    @DictData(label = "立即执行", sort = 2, style = DictDataStyle.INFO)
    public static final
    int IGNORE_MISFIRES = 2;

    @DictData(label = "执行一次", sort = 3, style = DictDataStyle.WARNING)
    public static final
    int FIRE_AND_PROCEED = 3;

    @DictData(label = "不立即执行", sort = 4, style = DictDataStyle.ERROR)
    public static final
    int DO_NOTHING = 4;

    public static boolean valid(int value) {
        return value == DEFAULT || value == IGNORE_MISFIRES || value == FIRE_AND_PROCEED || value == DO_NOTHING;
    }

}
