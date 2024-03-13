package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/8/1 14:33
 */
@Dict(key = "JobMisfire", value = "系统定时任务-错过执行策略")
public interface JobMisfire {

    @DictData(label = "默认策略", sort = 1, style = DictDataStyle.SUCCESS)
    int DEFAULT = 1;

    @DictData(label = "立即执行", sort = 2, style = DictDataStyle.INFO)
    int IGNORE_MISFIRES = 2;

    @DictData(label = "执行一次", sort = 3, style = DictDataStyle.WARNING)
    int FIRE_AND_PROCEED = 3;

    @DictData(label = "不立即执行", sort = 4, style = DictDataStyle.ERROR)
    int DO_NOTHING = 4;

    static boolean valid(int value) {
        return value == DEFAULT || value == IGNORE_MISFIRES || value == FIRE_AND_PROCEED || value == DO_NOTHING;
    }

}
