package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/8/1 14:41
 */
@Dict(key = "JobStatus", value = "系统定时任务-状态")
public interface JobStatus {

    @DictData(label = "正常", sort = 1, style = DictDataStyle.SUCCESS)
    int NORMAL = 1;

    @DictData(label = "暂停", sort = 2, style = DictDataStyle.WARNING)
    int PAUSE = 2;

    static boolean valid(int value) {
        return value == NORMAL || value == PAUSE;
    }

}
