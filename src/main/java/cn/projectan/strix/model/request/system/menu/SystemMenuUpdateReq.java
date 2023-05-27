package cn.projectan.strix.model.request.system.menu;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/6/20 19:02
 */
@Data
public class SystemMenuUpdateReq {

    /**
     * 菜单名称
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "菜单名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 10, message = "菜单名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * 访问地址
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "菜单路由不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 1, max = 48, message = "菜单路由长度不符合要求")
    @UpdateField
    private String url;

    /**
     * 菜单ICON
     */
    @UpdateField
    private String icon;

    /**
     * 父菜单ID
     */
    @UpdateField
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "菜单排序值不可为空")
    @Min(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, value = 0, message = "菜单排序值超出可用范围")
    @Max(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, value = 1000000, message = "菜单排序值超出可用范围")
    @UpdateField
    private Integer sortValue;

}
