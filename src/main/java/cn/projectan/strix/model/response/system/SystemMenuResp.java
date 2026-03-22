package cn.projectan.strix.model.response.system;

import cn.projectan.strix.model.db.system.SystemMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2021/5/13 18:53
 */
@Schema(description = "系统菜单响应")
@Getter
@NoArgsConstructor
public class SystemMenuResp {

    @Schema(description = "菜单列表")
    private final List<SystemMenuItem> menuList = new ArrayList<>();

    public SystemMenuResp(List<SystemMenu> menus) {
        Map<String, List<SystemMenu>> childMenuMap = menus.stream()
                .collect(Collectors.groupingBy(SystemMenu::getParentId));

        for (SystemMenu sm : childMenuMap.getOrDefault("0", Collections.emptyList())) {
            SystemMenuItem item = new SystemMenuItem(sm.getId(), sm.getName(), sm.getUrl(), sm.getIcon(), findChildren(childMenuMap, sm.getId()));
            menuList.add(item);
        }
    }

    private List<SystemMenuItem> findChildren(Map<String, List<SystemMenu>> childMenuMap, String id) {
        return childMenuMap.getOrDefault(id, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(SystemMenu::getSortValue))
                .map(m -> new SystemMenuItem(m.getId(), m.getName(), m.getUrl(), m.getIcon(), findChildren(childMenuMap, m.getId())))
                .collect(Collectors.toList());
    }

    @Schema(description = "系统菜单项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMenuItem {

        @Schema(description = "菜单ID")
        private String id;

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
         * 子菜单
         */
        @Schema(description = "子菜单列表")
        private List<SystemMenuItem> children;
    }

}
