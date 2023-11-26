package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemRoleMapper;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
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
        QueryWrapper<SystemRoleMenu> systemRoleMenuQueryWrapper = new QueryWrapper<>();
        systemRoleMenuQueryWrapper.select("system_menu_id");
        systemRoleMenuQueryWrapper.in("system_role_id", roleId);
        List<String> systemRoleMenuIds = systemRoleMenuService.listObjs(systemRoleMenuQueryWrapper, Object::toString);
        if (systemRoleMenuIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<SystemMenu> systemMenuQueryWrapper = new QueryWrapper<>();
        systemMenuQueryWrapper.in("id", systemRoleMenuIds);
        systemMenuQueryWrapper.orderByAsc("sort_value");
        return systemMenuService.list(systemMenuQueryWrapper);
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
        QueryWrapper<SystemRolePermission> systemRolePermissionQueryWrapper = new QueryWrapper<>();
        systemRolePermissionQueryWrapper.select("system_permission_id");
        systemRolePermissionQueryWrapper.in("system_role_id", roleId);
        List<String> systemPermissionIdList = systemRolePermissionService.listObjs(systemRolePermissionQueryWrapper, Object::toString);
        if (systemPermissionIdList.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<SystemPermission> systemPermissionQueryWrapper = new QueryWrapper<>();
        systemPermissionQueryWrapper.in("id", systemPermissionIdList);
        return systemPermissionService.list(systemPermissionQueryWrapper);
    }

}
