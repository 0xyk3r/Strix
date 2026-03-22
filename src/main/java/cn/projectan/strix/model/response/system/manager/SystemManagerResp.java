package cn.projectan.strix.model.response.system.manager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2021/7/16 16:15
 */
@Schema(description = "管理员详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerResp {

    @Schema(description = "管理员ID")
    private String id;

    @Schema(description = "管理员昵称")
    private String nickname;

    @Schema(description = "登录名")
    private String loginName;

    @Schema(description = "状态")
    private Short status;

    @Schema(description = "管理员类型")
    private Short type;

    @Schema(description = "所属区域ID")
    private String regionId;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "角色ID列表")
    private String roleIds;

}
