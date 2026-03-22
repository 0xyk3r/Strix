package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/26 18:24
 */
@Data
public class OssBucketListReq extends BasePageReq<OssBucket> {

    @Size(max = 64)
    private String keyword;

    private String configKey;

}
