package cn.projectan.strix.model.request.system.menu;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * @author ProjectAn
 * @since 2021/6/20 19:02
 */
@Schema(description = "菜单更新请求")
@FormSchema
@Data
public class SystemMenuUpdateReq {

    /**
     * 菜单权限标识
     */
    @Schema(description = "菜单权限标识", example = "system:user:list")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.menu.permKey}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "{validation.length:field.menu.permKey}")
    @UpdateField
    private String key;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称", example = "用户管理")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.menu.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 10, message = "{validation.length:field.menu.name}")
    @UpdateField
    private String name;

    /**
     * 访问地址
     */
    @Schema(description = "菜单路由地址", example = "/system/user")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.menu.route}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 128, message = "{validation.length:field.menu.route}")
    @UpdateField
    private String url;

    /**
     * 菜单ICON
     */
    @Schema(description = "菜单图标", example = "user")
    @UpdateField
    private String icon;

    /**
     * 父菜单ID
     */
    @Schema(description = "父菜单ID，顶级菜单为 0", example = "0")
    @UpdateField(allowEmpty = true, defaultValue = "0")
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.menu.sort}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.outOfRange:field.menu.sort}")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 1000000, message = "{validation.outOfRange:field.menu.sort}")
    @UpdateField
    private Integer sortValue;

}
