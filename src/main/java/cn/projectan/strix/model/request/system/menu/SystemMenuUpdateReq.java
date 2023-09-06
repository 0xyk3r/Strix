package cn.projectan.strix.model.request.system.menu;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
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
     * 菜单 Key
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "菜单Key不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "菜单Key长度不符合要求")
    @UpdateField
    private String key;

    /**
     * 菜单名称
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "菜单名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 10, message = "菜单名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * 访问地址
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "菜单路由不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 128, message = "菜单路由长度不符合要求")
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
    @UpdateField(allowEmpty = true, defaultValue = "0")
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "菜单排序值不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "菜单排序值超出可用范围")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 1000000, message = "菜单排序值超出可用范围")
    @UpdateField
    private Integer sortValue;

}
