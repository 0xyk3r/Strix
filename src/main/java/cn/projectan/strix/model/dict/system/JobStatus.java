package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author ProjectAn
 * @since 2023/8/1 14:41
 */
@Dict(key = "JobStatus", value = "系统定时任务-状态")
@Schema(description = "系统定时任务-状态")
public class JobStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short NORMAL = 1;

    @DictData(label = "暂停", sort = 2, style = DictDataStyle.WARNING)
    public static final
    short PAUSE = 2;

    public static boolean valid(short value) {
        return value == NORMAL || value == PAUSE;
    }

}
