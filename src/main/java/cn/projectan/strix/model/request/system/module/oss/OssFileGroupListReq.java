package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/26 19:14
 */
@Data
public class OssFileGroupListReq extends BasePageReq<OssFileGroup> {

    @Size(max = 64)
    private String keyword;

    private String configKey;

}
