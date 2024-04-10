package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/8/1 14:41
 */
@Component
@Dict(key = "JobStatus", value = "系统定时任务-状态")
public class JobStatus implements BaseDict {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int NORMAL = 1;

    @DictData(label = "暂停", sort = 2, style = DictDataStyle.WARNING)
    public static final
    int PAUSE = 2;

    public static boolean valid(int value) {
        return value == NORMAL || value == PAUSE;
    }

}
