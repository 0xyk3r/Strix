package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/23 11:57
 */
@Data
public class OssConfigListReq extends BasePageReq<OssConfig> {

    private String keyword;

}
