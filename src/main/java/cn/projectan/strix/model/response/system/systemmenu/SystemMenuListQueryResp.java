package cn.projectan.strix.model.response.system.systemmenu;

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
 * @author 安炯奕
 * @date 2021/6/19 22:57
 */
@Getter
@NoArgsConstructor
public class SystemMenuListQueryResp {

    private List<SystemMenuItem> systemMenuList = new ArrayList<>();

    public SystemMenuListQueryResp(List<SystemMenu> menus) {
        if (menus != null) {
            menus = menus.stream().sorted(Comparator.comparing(SystemMenu::getSortValue)).collect(Collectors.toList());
            for (SystemMenu sm : menus) {
                if ("0".equals(sm.getParentId())) {
                    SystemMenuItem item = new SystemMenuItem(sm.getId(), sm.getName(), sm.getUrl(), sm.getIcon(), sm.getSortValue(), findChildren(menus, sm.getId()));
                    systemMenuList.add(item);
                }
            }
        }
    }

    public List<SystemMenuItem> findChildren(List<SystemMenu> menus, String id) {
        return menus.stream().filter(m -> id.equals(m.getParentId())).sorted(Comparator.comparing(SystemMenu::getSortValue)).map(m -> new SystemMenuItem(m.getId(), m.getName(), m.getUrl(), m.getIcon(), m.getSortValue(), findChildren(menus, m.getId()))).collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMenuItem {

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
         * 排序值
         */
        private Integer sortValue;

        /**
         * 子菜单
         */
        private List<SystemMenuItem> children;
    }

}
