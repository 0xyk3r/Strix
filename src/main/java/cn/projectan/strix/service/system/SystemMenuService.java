package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import cn.projectan.strix.mapper.system.SystemMenuMapper;
import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.db.system.SystemPermission;
import cn.projectan.strix.model.db.system.SystemRoleMenu;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <p>
 * Strix 系统菜单 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMenuService extends ServiceImpl<SystemMenuMapper, SystemMenu> {

    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;

    /**
     * 根据菜单 key 列表查询菜单（按排序值升序）
     */
    public List<SystemMenu> listByKeys(List<String> keys) {
        return lambdaQuery()
                .in(SystemMenu::getKey, keys)
                .orderByAsc(SystemMenu::getSortValue)
                .list();
    }

    /**
     * 更新菜单图标
     *
     * @param menuId 菜单ID
     * @param icon   图标
     * @return 是否更新成功
     */
    public boolean updateIcon(String menuId, String icon) {
        return lambdaUpdate()
                .eq(SystemMenu::getId, menuId)
                .set(SystemMenu::getIcon, icon)
                .update();
    }

    /**
     * 获取树形数据
     *
     * @return 树形数据
     */
    public CommonTreeDataResp getTreeData() {
        List<SystemMenu> systemMenuList = lambdaQuery()
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        return new CommonTreeDataResp(systemMenuList, "id", "name", "parentId", "0");
    }

    /**
     * 根据 ID 集合删除菜单
     *
     * @param idList ID 集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<String> idList) {
        if (idList.isEmpty()) {
            return;
        }

        // 查找所有需要删除的菜单ID（包含子菜单）
        Set<String> menuIdsToDelete = findMenuChildrenIdList(list(), idList);
        if (menuIdsToDelete.isEmpty()) {
            return;
        }

        // 批量删除菜单
        baseMapper.deleteByIds(menuIdsToDelete);

        // 删除角色和菜单间关系
        systemRoleMenuService.lambdaUpdate()
                .in(SystemRoleMenu::getSystemMenuId, menuIdsToDelete)
                .remove();

        // 删除菜单关联的权限
        systemPermissionService.lambdaUpdate()
                .in(SystemPermission::getMenuId, menuIdsToDelete)
                .remove();

        // 更新缓存
        SpringUtil.getBean(SystemMenuCache.class).updateRamAndRedis();
        SpringUtil.getBean(SystemPermissionCache.class).updateRamAndRedis();
    }

    /**
     * 查找菜单子节点 ID 列表
     *
     * @param menus     菜单列表
     * @param parentIds 父节点 ID 列表
     * @return 子节点 ID 列表
     */
    private Set<String> findMenuChildrenIdList(List<SystemMenu> menus, Collection<String> parentIds) {
        if (parentIds == null || parentIds.isEmpty() || menus.isEmpty()) {
            return Collections.emptySet();
        }

        // 构建父子关系映射
        Map<String, List<String>> parentChildMap = new HashMap<>();
        Set<String> allMenuIds = new HashSet<>();

        for (SystemMenu menu : menus) {
            allMenuIds.add(menu.getId());
            parentChildMap
                    .computeIfAbsent(menu.getParentId(), k -> new ArrayList<>())
                    .add(menu.getId());
        }

        // 过滤合法 parentId
        Deque<String> queue = new ArrayDeque<>();
        Set<String> result = new HashSet<>();

        for (String parentId : parentIds) {
            if (allMenuIds.contains(parentId)) {
                result.add(parentId);
                queue.offer(parentId);
            }
        }

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            List<String> children = parentChildMap.get(currentId);

            if (children == null) {
                continue;
            }

            for (String childId : children) {
                if (result.add(childId)) {
                    queue.offer(childId);
                }
            }
        }

        return result;
    }

}
