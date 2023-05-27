package cn.projectan.strix.model.request.system.permission;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/7/20 15:44
 */
@Data
public class SystemPermissionUpdateReq {

    /**
     * 权限名称
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "权限名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 12, message = "权限名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * 权限标识
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "权限标识不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 32, message = "权限标识长度不符合要求")
    @UpdateField
    private String permissionKey;

    /**
     * 权限类型
     */
    @NotNull(groups = {ValidationGroup.Insert.class}, message = "权限类型未选择")
    @UpdateField
    private Integer permissionType;

    /**
     * 权限介绍
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "权限介绍不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 128, message = "权限介绍长度不符合要求")
    @UpdateField
    private String description;

}
