package cn.projectan.strix.model.response.system.menu;

import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.db.system.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2021/6/19 22:57
 */
@Schema(description = "菜单列表响应")
@Getter
@NoArgsConstructor
public class SystemMenuListResp {

    @Schema(description = "系统菜单列表")
    private final List<SystemMenuManageItem> systemMenuList = new ArrayList<>();

    public SystemMenuListResp(List<SystemMenu> menus, List<SystemPermission> permissions) {
        if (menus != null && !menus.isEmpty()) {
            Map<String, List<SystemMenu>> childMenuMap = menus.stream()
                    .collect(Collectors.groupingBy(SystemMenu::getParentId));
            Map<String, List<SystemPermission>> permByMenuId = permissions.stream()
                    .collect(Collectors.groupingBy(SystemPermission::getMenuId));

            childMenuMap.getOrDefault("0", Collections.emptyList()).stream()
                    .sorted(Comparator.comparing(SystemMenu::getSortValue))
                    .forEach(m -> systemMenuList.add(new SystemMenuManageItem("menu", m.getId(), m.getKey(), m.getName(), m.getUrl(), m.getIcon(), m.getSortValue(), findChildren(childMenuMap, permByMenuId, m.getId()))));
        }
    }

    private List<SystemMenuManageItem> findChildren(Map<String, List<SystemMenu>> childMenuMap, Map<String, List<SystemPermission>> permByMenuId, String id) {
        List<SystemMenuManageItem> children = new ArrayList<>();

        // 查找子菜单
        childMenuMap.getOrDefault(id, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(SystemMenu::getSortValue))
                .forEach(m -> children.add(new SystemMenuManageItem("menu", m.getId(), m.getKey(), m.getName(), m.getUrl(), m.getIcon(), m.getSortValue(), findChildren(childMenuMap, permByMenuId, m.getId()))));

        // 查找子权限
        permByMenuId.getOrDefault(id, Collections.emptyList())
                .forEach(p -> children.add(new SystemMenuManageItem("permission", p.getId(), p.getKey(), p.getName(), null, null, null, null)));

        return children;
    }

    @Schema(description = "系统菜单管理项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMenuManageItem {

        /**
         * 数据类型 menu 菜单 | permission 权限
         */
        @Schema(description = "数据类型（menu-菜单/permission-权限）")
        private String type;

        @Schema(description = "菜单/权限ID")
        private String id;

        /**
         * 菜单/权限 Key
         */
        @Schema(description = "菜单/权限标识")
        private String key;

        /**
         * 菜单/权限 名称
         */
        @Schema(description = "菜单/权限名称")
        private String name;

        /**
         * 访问地址
         */
        @Schema(description = "访问地址")
        private String url;

        /**
         * 菜单 ICON
         */
        @Schema(description = "菜单图标")
        private String icon;

        /**
         * 排序值
         */
        @Schema(description = "排序值")
        private Integer sortValue;

        /**
         * 子菜单
         */
        @Schema(description = "子菜单列表")
        private List<SystemMenuManageItem> children;

        @Schema(description = "是否为叶子节点")
        private boolean isLeaf;

        public boolean getIsLeaf() {
            return children == null || children.isEmpty();
        }

        public SystemMenuManageItem(String type, String id, String key, String name, String url, String icon, Integer sortValue, List<SystemMenuManageItem> children) {
            this.type = type;
            this.id = id;
            this.key = key;
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.sortValue = sortValue;
            this.children = children;
        }

    }

}
