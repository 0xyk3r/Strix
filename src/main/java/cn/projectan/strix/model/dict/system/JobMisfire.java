package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2023/8/1 14:33
 */
@Dict(key = "JobMisfire", value = "系统定时任务-错过执行策略")
@Schema(description = "系统定时任务-错过执行策略")
public class JobMisfire implements BaseDict {

    @DictData(label = "默认策略", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short DEFAULT = 1;

    @DictData(label = "立即执行", sort = 2, style = DictDataStyle.INFO)
    public static final
    short IGNORE_MISFIRES = 2;

    @DictData(label = "执行一次", sort = 3, style = DictDataStyle.WARNING)
    public static final
    short FIRE_AND_PROCEED = 3;

    @DictData(label = "不立即执行", sort = 4, style = DictDataStyle.ERROR)
    public static final
    short DO_NOTHING = 4;

    public static boolean valid(short value) {
        return value == DEFAULT || value == IGNORE_MISFIRES || value == FIRE_AND_PROCEED || value == DO_NOTHING;
    }

}
