package cn.projectan.strix.model.request.system.region;

import cn.projectan.strix.model.db.system.SystemRegion;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/9/29 17:56
 */
@Data
public class SystemRegionListReq extends BasePageReq<SystemRegion> {

    @Size(max = 64)
    private String keyword;

    private String parentId;

}
