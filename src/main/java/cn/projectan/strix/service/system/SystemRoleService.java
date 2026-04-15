package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemRoleMapper;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.event.cache.RoleChangedEvent;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix 系统角色 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleService extends ServiceImpl<SystemRoleMapper, SystemRole> {

    private final SystemMenuService systemMenuService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询全部角色（按创建时间升序）
     */
    public List<SystemRole> listAll() {
        return lambdaQuery()
                .orderByAsc(SystemRole::getCreatedTime)
                .list();
    }

    /**
     * 查询全部角色（仅 id、name，用于穿梭框）
     */
    public List<SystemRole> listForTransfer() {
        return lambdaQuery()
                .select(SystemRole::getId, SystemRole::getName)
                .list();
    }

    /**
     * 获取下拉框数据 （有缓存）
     *
     * @return 下拉框数据
     */
    @Cacheable(value = "strix:system:role:select_data")
    public CommonSelectDataResp getSelectData() {
        List<SystemRole> systemRoleList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(systemRoleList);
    }

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 该角色具有的菜单权限
     */
    @Cacheable(value = "strix:system:role:menu_by_rid", key = "#roleId")
    public List<SystemMenu> getMenusByRoleId(String roleId) {
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add(roleId);
        return getMenusByRoleId(treeSet);
    }

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 该角色具有的菜单权限
     */
    @Cacheable(value = "strix:system:role:menu_by_rid", key = "#roleId")
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

    /**
     * 根据角色ID获取系统权限
     *
     * @param roleId 角色id
     * @return 该角色具有的系统权限
     */
    @Cacheable(value = "strix:system:role:permission_by_rid", key = "#roleId")
    public List<SystemPermission> getSystemPermissionByRoleId(String roleId) {
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add(roleId);
        return getSystemPermissionByRoleId(treeSet);
    }

    /**
     * 根据角色ID获取系统权限
     *
     * @param roleId 角色id
     * @return 该角色具有的系统权限
     */
    @Cacheable(value = "strix:system:role:permission_by_rid", key = "#roleId")
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

    /**
     * 删除角色及其所有关联关系（管理人员-角色、角色-菜单、角色-权限）
     *
     * @param systemRole 待删除的角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleWithRelations(SystemRole systemRole) {
        removeById(systemRole);
        String roleId = systemRole.getId();
        systemManagerRoleService.lambdaUpdate()
                .eq(SystemManagerRole::getSystemRoleId, roleId)
                .remove();
        systemRoleMenuService.lambdaUpdate()
                .eq(SystemRoleMenu::getSystemRoleId, roleId)
                .remove();
        systemRolePermissionService.lambdaUpdate()
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .remove();

        eventPublisher.publishEvent(new RoleChangedEvent(this, roleId));
    }

}
