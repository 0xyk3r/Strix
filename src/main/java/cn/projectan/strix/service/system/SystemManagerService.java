package cn.projectan.strix.service.system;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.system.SystemManagerMapper;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.dict.system.SystemManagerType;
import cn.projectan.strix.service.base.NameFetcherService;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemManagerService extends ServiceImpl<SystemManagerMapper, SystemManager> implements NameFetcherService<SystemManager> {

    private final SystemRoleService systemRoleService;
    private final SystemMenuService systemMenuService;
    private final SystemRegionService systemRegionService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final RedisUtil redisUtil;

    /**
     * 根据角色 ID 获取人员 ID 列表
     *
     * @param roleId 角色 ID
     * @return 管理人员 ID 列表
     */
    public List<String> getManagerIdListByRoleId(String roleId) {
        return systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemManagerId)
                .eq(SystemManagerRole::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .collect(Collectors.toList());
    }

    /**
     * 根据人员 ID 获取角色 ID 列表
     *
     * @param managerId 管理人员 ID
     * @return 角色 ID 列表
     */
    public List<String> getRoleIdListByManagerId(String managerId) {
        return systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemRoleId)
                .eq(SystemManagerRole::getSystemManagerId, managerId)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemRoleId)
                .collect(Collectors.toList());
    }

    /**
     * 获取管理人员的所有菜单权限
     *
     * @param managerId 管理人员 ID
     * @return 菜单权限列表
     */
    @Cacheable(value = "strix:system:manager:menu_by_mid", key = "#managerId")
    public List<String> getMenuKeyList(String managerId) {
        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(managerId);

        return systemRoleService.getMenusByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemMenu::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取管理人员的所有系统权限
     *
     * @param managerId 管理人员 ID
     * @return 系统权限列表
     */
    @Cacheable(value = "strix:system:manager:permission_by_mid", key = "#managerId")
    public List<String> getPermissionKeyList(String managerId) {
        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(managerId);

        return systemRoleService.getSystemPermissionByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemPermission::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 登陆时获取用户完整权限信息
     *
     * @param managerId 管理用户 ID
     * @return 登陆用户信息
     */
    public LoginSystemManager getLoginInfo(String managerId) {
        SystemManager systemManager = getById(managerId);
        SystemRegion systemRegion = null;
        if (StringUtils.hasText(systemManager.getRegionId())) {
            systemRegion = systemRegionService.getById(systemManager.getRegionId());
        }

        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(managerId);

        // 获取地区权限类型
        short regionPermissionType = 0;
        if (!CollectionUtils.isEmpty(systemManagerRoleIdList)) {
            regionPermissionType = Optional.ofNullable(
                            systemRoleService.lambdaQuery()
                                    .select(SystemRole::getRegionPermissionType)
                                    .in(SystemRole::getId, systemManagerRoleIdList)
                                    .orderByAsc(SystemRole::getRegionPermissionType)
                                    .last("limit 1")
                                    .one())
                    .map(SystemRole::getRegionPermissionType)
                    .orElse((short) 0);
        }

        List<String> menus;
        List<String> permissions;
        List<String> regionIds = null;
        if (systemManager.getType() == SystemManagerType.SUPER_ACCOUNT) {
            // 超级账号默认拥有所有权限
            menus = systemMenuService.list()
                    .stream().map(SystemMenu::getKey).collect(Collectors.toList());
            permissions = systemPermissionService.list()
                    .stream().map(SystemPermission::getKey).collect(Collectors.toList());
        } else {
            // 普通账号
            SystemManagerService proxy = SpringUtil.getAopProxy(this);
            menus = proxy.getMenuKeyList(systemManager.getId());
            permissions = proxy.getPermissionKeyList(systemManager.getId());
            if (StringUtils.hasText(systemManager.getRegionId())) {
                regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
            }
        }
        return new LoginSystemManager(systemManager, systemRegion, regionPermissionType, menus, permissions, regionIds);
    }

    /**
     * 根据管理人员 ID, 刷新 redis 中的权限信息
     *
     * @param managerId 管理人员 ID
     */
    public void refreshLoginInfoByManager(String managerId) {
        Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
        if (existToken != null) {
            LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
            redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
        }
    }

    /**
     * 根据角色 ID, 刷新 redis 中的权限信息
     *
     * @param roleId 角色 ID
     */
    public void refreshLoginInfoByRole(String roleId) {
        getManagerIdListByRoleId(roleId).forEach(managerId -> {
            Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
            if (existToken != null) {
                LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
                redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
            }
        });
    }

    /**
     * 根据角色 ID 列表, 刷新 redis 中的权限信息
     *
     * @param roleIdList 角色 ID 列表
     */
    public void refreshLoginInfoByRole(List<String> roleIdList) {
        systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemManagerId)
                .in(SystemManagerRole::getSystemRoleId, roleIdList)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .forEach(managerId -> {
                    Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
                    if (existToken != null) {
                        LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
                        redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
                    }
                });
    }

    /**
     * 根据菜单 ID, 刷新 redis 中的权限信息
     *
     * @param menuId 菜单 ID
     */
    public void refreshLoginInfoByMenu(String menuId) {
        List<String> roleIdList = systemRoleMenuService.lambdaQuery()
                .select(SystemRoleMenu::getSystemRoleId)
                .eq(SystemRoleMenu::getSystemMenuId, menuId)
                .list()
                .stream()
                .map(SystemRoleMenu::getSystemRoleId)
                .collect(Collectors.toList());

        if (!roleIdList.isEmpty()) {
            refreshLoginInfoByRole(roleIdList);
        }
    }

    /**
     * 根据权限 ID, 刷新 redis 中的权限信息
     *
     * @param permissionId 权限 ID
     */
    public void refreshLoginInfoByPermission(String permissionId) {
        List<String> roleIdList = systemRolePermissionService.lambdaQuery()
                .select(SystemRolePermission::getSystemRoleId)
                .eq(SystemRolePermission::getSystemPermissionId, permissionId)
                .list()
                .stream()
                .map(SystemRolePermission::getSystemRoleId)
                .collect(Collectors.toList());

        if (!roleIdList.isEmpty()) {
            refreshLoginInfoByRole(roleIdList);
        }
    }

    @Override
    public String getDataNameById(String id) {
        SystemManager data = lambdaQuery()
                .select(SystemManager::getNickname)
                .eq(SystemManager::getId, id)
                .one();
        return data == null ? null : data.getNickname();
    }

}
