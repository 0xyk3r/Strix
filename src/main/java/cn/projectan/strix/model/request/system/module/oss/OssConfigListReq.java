package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/23 11:57
 */
@Data
public class OssConfigListReq extends BasePageReq<OssConfig> {

    @Size(max = 64)
    private String keyword;

}
