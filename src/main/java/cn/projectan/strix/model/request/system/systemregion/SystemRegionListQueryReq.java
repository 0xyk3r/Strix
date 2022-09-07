package cn.projectan.strix.model.request.system.systemregion;

import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/9/29 17:56
 */
@Data
public class SystemRegionListQueryReq extends BasePageQueryReq<SystemRegion> {

    private String keyword;

    private String parentId;

}
