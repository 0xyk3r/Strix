package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.mapper.SystemMenuMapper;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRoleMenu;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.service.SystemRoleMenuService;
import cn.projectan.strix.util.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMenuServiceImpl extends ServiceImpl<SystemMenuMapper, SystemMenu> implements SystemMenuService {

    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;

    @Override
    public CommonTreeDataResp getTreeData() {
        List<SystemMenu> systemMenuList = lambdaQuery()
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        return new CommonTreeDataResp(systemMenuList, "id", "name", "parentId", "0");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<String> idList) {
        if (idList.isEmpty()) {
            return;
        }

        // 查找子菜单
        List<SystemMenu> allMenuList = list();
        Set<String> menuChildrenIdList = findMenuChildrenIdList(allMenuList, idList);

        // 删除菜单
        baseMapper.deleteByIds(menuChildrenIdList);

        // 删除角色和菜单间关系
        systemRoleMenuService.lambdaUpdate()
                .in(SystemRoleMenu::getSystemMenuId, menuChildrenIdList)
                .remove();
        // 删除菜单对应的权限
        Set<String> permissionsIdList = systemPermissionService.lambdaQuery()
                .in(SystemPermission::getMenuId, menuChildrenIdList)
                .list()
                .stream()
                .map(SystemPermission::getId)
                .collect(Collectors.toSet());
        systemPermissionService.deleteByIds(permissionsIdList);

        // 更新缓存
        SpringUtil.getBean(SystemMenuCache.class).updateRamAndRedis();
        SpringUtil.getBean(SystemPermissionCache.class).updateRamAndRedis();
    }

    private Set<String> findMenuChildrenIdList(List<SystemMenu> menus, Collection<String> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return new HashSet<>();
        }

        // 构建父子关系映射
        Map<String, List<String>> parentChildMap = new HashMap<>();
        Set<String> allMenuIds = new HashSet<>();

        for (SystemMenu menu : menus) {
            allMenuIds.add(menu.getId());
            parentChildMap.computeIfAbsent(menu.getParentId(), k -> new ArrayList<>()).add(menu.getId());
        }

        // 验证所有 parentId 是否存在
        Set<String> validParentIds = new HashSet<>();
        for (String parentId : parentIds) {
            if (allMenuIds.contains(parentId)) {
                validParentIds.add(parentId);
            }
        }

        if (validParentIds.isEmpty()) {
            return new HashSet<>();
        }

        // 使用队列进行广度优先搜索
        Set<String> result = new HashSet<>(validParentIds);
        Queue<String> queue = new LinkedList<>(validParentIds);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            List<String> children = parentChildMap.get(currentId);

            if (children != null) {
                for (String childId : children) {
                    if (result.add(childId)) {
                        queue.offer(childId);
                    }
                }
            }
        }

        return result;
    }

}
