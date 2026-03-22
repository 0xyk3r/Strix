package cn.projectan.strix.model.request.system.region;

import cn.projectan.strix.model.db.system.SystemRegion;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/9/29 17:56
 */
@Schema(description = "地区列表请求")
@Data
public class SystemRegionListReq extends BasePageReq<SystemRegion> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "父级区划ID")
    private String parentId;

}
