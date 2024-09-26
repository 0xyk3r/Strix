package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemRoleMapper;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Service
@RequiredArgsConstructor
public class SystemRoleServiceImpl extends ServiceImpl<SystemRoleMapper, SystemRole> implements SystemRoleService {

    private final SystemMenuService systemMenuService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;
    private final SystemRolePermissionService systemRolePermissionService;

    @Cacheable(value = "strix:system:role:select_data")
    @Override
    public CommonSelectDataResp getSelectData() {
        List<SystemRole> systemRoleList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(systemRoleList);
    }

    @Cacheable(value = "strix:system:role:menu_by_rid", key = "#roleId")
    @Override
    public List<SystemMenu> getMenusByRoleId(String roleId) {
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add(roleId);
        return getMenusByRoleId(treeSet);
    }

    @Cacheable(value = "strix:system:role:menu_by_rid", key = "#roleId")
    @Override
    public List<SystemMenu> getMenusByRoleId(SortedSet<String> roleId) {
        if (roleId.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> systemRoleMenuIds = systemRoleMenuService.lambdaQuery()
                .select(SystemRoleMenu::getSystemMenuId)
                .in(SystemRoleMenu::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRoleMenu::getSystemMenuId)
                .collect(Collectors.toList());
        if (systemRoleMenuIds.isEmpty()) {
            return new ArrayList<>();
        }
        return systemMenuService.lambdaQuery()
                .in(SystemMenu::getId, systemRoleMenuIds)
                .orderByAsc(SystemMenu::getSortValue)
                .list();
    }

    @Cacheable(value = "strix:system:role:permission_by_rid", key = "#roleId")
    @Override
    public List<SystemPermission> getSystemPermissionByRoleId(String roleId) {
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add(roleId);
        return getSystemPermissionByRoleId(treeSet);
    }

    @Cacheable(value = "strix:system:role:permission_by_rid", key = "#roleId")
    @Override
    public List<SystemPermission> getSystemPermissionByRoleId(SortedSet<String> roleId) {
        if (roleId.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> systemPermissionIdList = systemRolePermissionService.lambdaQuery()
                .select(SystemRolePermission::getSystemPermissionId)
                .in(SystemRolePermission::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRolePermission::getSystemPermissionId)
                .collect(Collectors.toList());
        if (systemPermissionIdList.isEmpty()) {
            return new ArrayList<>();
        }
        return systemPermissionService.lambdaQuery()
                .in(SystemPermission::getId, systemPermissionIdList)
                .list();
    }

}
