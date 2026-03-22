package cn.projectan.strix.model.request.system.manager;

import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/6/11 18:02
 */
@Schema(description = "管理员列表请求")
@Data
public class SystemManagerListReq extends BasePageReq<SystemManager> {

    @Schema(description = "搜索关键词", example = "张三")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "管理员状态", example = "1")
    private Short status;

    @Schema(description = "管理员类型", example = "1")
    private Short type;

    @Schema(description = "地区ID")
    private String regionId;

    @Schema(description = "角色ID")
    private String roleId;

}
