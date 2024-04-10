package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/5/23 8:53
 */
@Component
@Dict(key = "StrixOssFileGroupSecretType", value = "存储服务-文件组-权限类型")
public class StrixOssFileGroupSecretType implements BaseDict {

    @DictData(label = "管理端文件", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    int MANAGER = 1;

    @DictData(label = "用户端文件", sort = 2, style = DictDataStyle.INFO)
    public static final
    int USER = 2;

}
