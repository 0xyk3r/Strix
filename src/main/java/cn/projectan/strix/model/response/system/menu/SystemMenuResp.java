package cn.projectan.strix.model.response.system.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/6/20 20:04
 */
@Schema(description = "菜单详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMenuResp {

    @Schema(description = "菜单ID")
    private String id;

    /**
     * 菜单权限标识
     */
    @Schema(description = "菜单权限标识")
    private String key;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    private String name;

    /**
     * 访问地址
     */
    @Schema(description = "访问地址")
    private String url;

    /**
     * 菜单ICON
     */
    @Schema(description = "菜单图标")
    private String icon;

    /**
     * 父菜单ID
     */
    @Schema(description = "父菜单ID")
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    @Schema(description = "排序值，越小越靠前")
    private Integer sortValue;

}
