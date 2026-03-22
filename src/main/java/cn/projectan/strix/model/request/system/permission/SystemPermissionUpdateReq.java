package cn.projectan.strix.model.request.system.permission;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/7/20 15:44
 */
@Schema(description = "权限更新请求")
@Data
public class SystemPermissionUpdateReq {

    /**
     * 权限名称
     */
    @Schema(description = "权限名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "权限名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 12, message = "权限名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * 权限标识
     */
    @Schema(description = "权限标识")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "权限标识不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "权限标识长度不符合要求")
    @UpdateField
    private String key;

    /**
     * 所属菜单 ID
     */
    @Schema(description = "所属菜单ID")
    @UpdateField
    private String menuId;

    /**
     * 权限介绍
     */
    @Schema(description = "权限介绍")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128, message = "权限介绍长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String description;

}
