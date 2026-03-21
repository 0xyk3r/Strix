package cn.projectan.strix.model.response.system.menu;

import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.db.system.SystemPermission;
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
@Getter
@NoArgsConstructor
public class SystemMenuListResp {

    private final List<SystemMenuItem> systemMenuList = new ArrayList<>();

    public SystemMenuListResp(List<SystemMenu> menus, List<SystemPermission> permissions) {
        if (menus != null && !menus.isEmpty()) {
            Map<String, List<SystemMenu>> childMenuMap = menus.stream()
                    .collect(Collectors.groupingBy(SystemMenu::getParentId));
            Map<String, List<SystemPermission>> permByMenuId = permissions.stream()
                    .collect(Collectors.groupingBy(SystemPermission::getMenuId));

            childMenuMap.getOrDefault("0", Collections.emptyList()).stream()
                    .sorted(Comparator.comparing(SystemMenu::getSortValue))
                    .forEach(m -> systemMenuList.add(new SystemMenuItem("menu", m.getId(), m.getKey(), m.getName(), m.getUrl(), m.getIcon(), m.getSortValue(), findChildren(childMenuMap, permByMenuId, m.getId()))));
        }
    }

    private List<SystemMenuItem> findChildren(Map<String, List<SystemMenu>> childMenuMap, Map<String, List<SystemPermission>> permByMenuId, String id) {
        List<SystemMenuItem> children = new ArrayList<>();

        // 查找子菜单
        childMenuMap.getOrDefault(id, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(SystemMenu::getSortValue))
                .forEach(m -> children.add(new SystemMenuItem("menu", m.getId(), m.getKey(), m.getName(), m.getUrl(), m.getIcon(), m.getSortValue(), findChildren(childMenuMap, permByMenuId, m.getId()))));

        // 查找子权限
        permByMenuId.getOrDefault(id, Collections.emptyList())
                .forEach(p -> children.add(new SystemMenuItem("permission", p.getId(), p.getKey(), p.getName(), null, null, null, null)));

        return children;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMenuItem {

        /**
         * 数据类型 menu 菜单 | permission 权限
         */
        private String type;

        private String id;

        /**
         * 菜单/权限 Key
         */
        private String key;

        /**
         * 菜单/权限 名称
         */
        private String name;

        /**
         * 访问地址
         */
        private String url;

        /**
         * 菜单 ICON
         */
        private String icon;

        /**
         * 排序值
         */
        private Integer sortValue;

        /**
         * 子菜单
         */
        private List<SystemMenuItem> children;

        private boolean isLeaf;

        public boolean getIsLeaf() {
            return children == null || children.isEmpty();
        }

        public SystemMenuItem(String type, String id, String key, String name, String url, String icon, Integer sortValue, List<SystemMenuItem> children) {
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
