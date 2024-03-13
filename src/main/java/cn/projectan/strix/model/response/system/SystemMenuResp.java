package cn.projectan.strix.model.response.system;

import cn.projectan.strix.model.db.SystemMenu;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @date 2021/5/13 18:53
 */
@Getter
@NoArgsConstructor
public class SystemMenuResp {

    private final List<SystemMenuItem> menuList = new ArrayList<>();

    public SystemMenuResp(List<SystemMenu> menus) {
        for (SystemMenu sm : menus) {
            if ("0".equals(sm.getParentId())) {
                SystemMenuItem item = new SystemMenuItem(sm.getId(), sm.getName(), sm.getUrl(), sm.getIcon(), findChildren(menus, sm.getId()));
                menuList.add(item);
            }
        }
    }

    private List<SystemMenuItem> findChildren(List<SystemMenu> menus, String id) {
        return menus.stream().filter(m -> id.equals(m.getParentId())).sorted(Comparator.comparing(SystemMenu::getSortValue)).map(m -> new SystemMenuItem(m.getId(), m.getName(), m.getUrl(), m.getIcon(), findChildren(menus, m.getId()))).collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SystemMenuItem {

        private String id;

        /**
         * 菜单名称
         */
        private String name;

        /**
         * 访问地址
         */
        private String url;

        /**
         * 菜单ICON
         */
        private String icon;

        /**
         * 子菜单
         */
        private List<SystemMenuItem> children;
    }

}
