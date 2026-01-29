package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2023/5/23 8:53
 */
@Dict(key = "OssFileGroupSecretType", value = "存储服务-文件组-权限类型")
public class OssFileGroupSecretType implements BaseDict {

    @DictData(label = "管理端文件", sort = 1, style = DictDataStyle.SUCCESS)
    public static final
    short MANAGER = 1;

    @DictData(label = "用户端文件", sort = 2, style = DictDataStyle.INFO)
    public static final
    short USER = 2;

}
