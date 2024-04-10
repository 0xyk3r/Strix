package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * 系统管理用户 类型
 *
 * @author ProjectAn
 * @date 2021/6/16 15:32
 */
@Component
@Dict(key = "SystemManagerType", value = "系统人员-类型")
public class SystemManagerType implements BaseDict {

    @DictData(label = "超级账号", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int SUPER_ACCOUNT = 1;

    @DictData(label = "普通账号", sort = 2, style = DictDataStyle.PRIMARY)
    public static final
    int NORMAL_ACCOUNT = 2;

    public static boolean valid(int type) {
        return type == SUPER_ACCOUNT || type == NORMAL_ACCOUNT;
    }

}
