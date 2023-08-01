package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/5/23 8:53
 */
@Dict(key = "StrixOssFileGroupSecretType", value = "存储服务-文件组-权限类型")
public interface StrixOssFileGroupSecretType {

    @DictData(label = "管理端文件", sort = 1, style = DictDataStyle.SUCCESS)
    int MANAGER = 1;

    @DictData(label = "用户端文件", sort = 2, style = DictDataStyle.INFO)
    int USER = 2;

}
