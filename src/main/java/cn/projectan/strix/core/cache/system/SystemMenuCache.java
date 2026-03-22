package cn.projectan.strix.core.cache.system;

import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.service.system.SystemMenuService;
import cn.projectan.strix.util.common.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统菜单缓存
 *
 * @author ProjectAn
 * @since 2021/5/13 18:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMenuCache {

    private final RedisUtil redisUtil;
    private final SystemMenuService systemMenuService;

    private record MenuCacheData(List<SystemMenu> menus, Map<String, List<SystemMenu>> childrenByParentId) {
    }

    private volatile MenuCacheData cache = new MenuCacheData(List.of(), Map.of());

    @PostConstruct
    private void init() {
        List<SystemMenu> menus = systemMenuService.lambdaQuery()
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        Map<String, List<SystemMenu>> childrenByParentId = menus.stream()
                .collect(Collectors.groupingBy(SystemMenu::getParentId));
        cache = new MenuCacheData(menus, childrenByParentId);
        log.info("Strix Cache: 管理系统菜单缓存加载完成, 缓存了 {} 个菜单.", menus.size());
    }

    public List<String> getIdListByParentMenu(String menuId) {
        List<String> result = new ArrayList<>();
        result.add(menuId);
        Map<String, List<SystemMenu>> childrenByParentId = cache.childrenByParentId();
        for (SystemMenu child : childrenByParentId.getOrDefault(menuId, Collections.emptyList())) {
            result.addAll(getIdListByParentMenu(child.getId()));
        }
        return result;
    }

    public void updateRam() {
        init();
    }

    public void updateRedis() {
        redisUtil.delLike("strix:system:manager:menu_by_mid:*");
        redisUtil.delLike("strix:system:role:menu_by_rid:*");
    }

    public void updateRedisBySystemRoleId(String roleId) {
        redisUtil.delLike("strix:system:role:menu_by_rid::" + roleId);
        redisUtil.delLike("strix:system:manager:menu_by_mid:*");
    }

    public void updateRedisBySystemManageId(String managerId) {
        redisUtil.delLike("strix:system:manager:menu_by_mid::" + managerId);
    }

    public void updateRamAndRedis() {
        updateRam();
        updateRedis();
    }

}
