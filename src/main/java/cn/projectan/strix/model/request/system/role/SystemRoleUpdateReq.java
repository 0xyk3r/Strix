package cn.projectan.strix.model.request.system.role;

import cn.projectan.strix.core.validation.annotation.ConstantDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.dict.system.SystemRoleRegionPermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/7/1 17:14
 */
@Schema(description = "角色更新请求")
@Data
public class SystemRoleUpdateReq {

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.role.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16, message = "{validation.length:field.role.name}")
    @UpdateField
    private String name;

    /**
     * 地区权限类型
     *
     * @see SystemRoleRegionPermissionType
     */
    @Schema(description = "地区权限类型")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.role.regionPermType}")
    @ConstantDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dict = SystemRoleRegionPermissionType.class, message = "{validation.invalid:field.role.regionPermType}")
    @UpdateField
    private Short regionPermissionType;

}
