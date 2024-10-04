package cn.projectan.strix.model.request.system.region;

import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/9/29 17:56
 */
@Data
public class SystemRegionListReq extends BasePageReq<SystemRegion> {

    private String keyword;

    private String parentId;

}
