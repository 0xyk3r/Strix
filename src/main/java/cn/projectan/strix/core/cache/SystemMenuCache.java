package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2021/5/13 18:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMenuCache {

    private final SystemMenuService systemMenuService;
    private final RedisUtil redisUtil;

    private List<SystemMenu> instance = new ArrayList<>();

    @PostConstruct
    private void init() {
        QueryWrapper<SystemMenu> systemMenuQueryWrapper = new QueryWrapper<>();
        systemMenuQueryWrapper.orderByAsc("sort_value");
        instance = systemMenuService.list(systemMenuQueryWrapper);
        log.info(String.format("Strix Cache: 管理系统菜单缓存加载成功, 缓存了 %s 个菜单.", instance.size()));
    }

    public List<String> getIdListByParentMenu(String menuId) {
        List<String> result = new ArrayList<>();
        result.add(menuId);
        instance.forEach(m -> {
            if (m.getParentId().equals(menuId)) {
                result.addAll(getIdListByParentMenu(m.getId()));
            }
        });
        return result;
    }

    public void updateRam() {
        init();
    }

    public void updateRedis() {
        redisUtil.delKeys("strix:system:manager:menu_by_smid:*");
        redisUtil.delKeys("strix:system:role:menu_by_rid:*");
    }

    public void updateRedisBySystemRoleId(String roleId) {
        redisUtil.delKeys("strix:system:role:menu_by_rid::" + roleId);
        // TODO 可优化为仅清除拥有该角色的管理用户缓存
        redisUtil.delKeys("strix:system:manager:menu_by_smid:*");
    }

    public void updateRedisBySystemManageId(String managerId) {
        redisUtil.delKeys("strix:system:manager:menu_by_smid::" + managerId);
    }

    public void updateRamAndRedis() {
        updateRam();
        updateRedis();
    }

}
