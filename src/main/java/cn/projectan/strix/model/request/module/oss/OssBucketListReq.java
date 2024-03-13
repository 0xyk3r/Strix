package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/26 18:24
 */
@Data
public class OssBucketListReq extends BasePageReq<OssBucket> {

    private String keyword;

    private String configKey;

}
