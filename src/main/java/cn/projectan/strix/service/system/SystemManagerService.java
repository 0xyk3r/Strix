package cn.projectan.strix.service.system;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.system.SystemManagerMapper;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.dict.system.SystemManagerType;
import cn.projectan.strix.model.dict.system.SystemRoleRegionPermissionType;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.manager.SystemManagerListReq;
import cn.projectan.strix.service.base.NameFetcherService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.SpringUtil;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private final TokenSessionService tokenSessionService;

    /**
     * 根据登录名查询管理人员
     */
    public SystemManager getByLoginName(String loginName) {
        return lambdaQuery()
                .eq(SystemManager::getLoginName, loginName)
                .one();
    }

    /**
     * 分页查询管理人员列表
     */
    public Page<SystemManager> listPage(SystemManagerListReq req, List<String> regionPermissions) {
        return lambdaQuery()
                .eq(StringUtils.hasText(req.getKeyword()), SystemManager::getNickname, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SystemManager::getLoginName, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), SystemManager::getStatus, req.getStatus())
                .eq(NumUtil.checkCategory(req.getType(), NumCategory.POSITIVE), SystemManager::getType, req.getType())
                .in(!CollectionUtils.isEmpty(regionPermissions), SystemManager::getRegionId, regionPermissions)
                .orderByAsc(SystemManager::getCreatedTime)
                .page(req.getPage());
    }

    /**
     * 查询全部管理人员（仅 id、nickname，用于穿梭框）
     */
    public List<SystemManager> listForTransfer() {
        return lambdaQuery()
                .select(SystemManager::getId, SystemManager::getNickname)
                .list();
    }

    /**
     * 批量查询管理人员头像配置（仅 id、avatarConfig）
     *
     * @param managerIds 管理人员 ID 列表
     * @return ID -> 头像配置 JSON 映射（无配置的为 null）
     */
    public Map<String, String> listAvatarConfigByIds(List<String> managerIds) {
        if (CollectionUtils.isEmpty(managerIds)) {
            return Map.of();
        }
        List<SystemManager> list = lambdaQuery()
                .select(SystemManager::getId, SystemManager::getAvatarConfig)
                .in(SystemManager::getId, managerIds)
                .list();
        // 使用 HashMap 而非 Collectors.toMap，避免 avatarConfig 为 null 时抛 NPE
        Map<String, String> result = new HashMap<>(list.size());
        list.forEach(m -> result.put(m.getId(), m.getAvatarConfig()));
        return result;
    }

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
     * 根据角色 ID 列表批量获取人员 ID 列表
     *
     * @param roleIds 角色 ID 列表
     * @return 管理人员 ID 列表
     */
    public List<String> getManagerIdListByRoleIds(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemManagerId)
                .in(SystemManagerRole::getSystemRoleId, roleIds)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .distinct()
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
        Assert.notNull(systemManager, I18nUtil.notFound("field.systemManager") + ": " + managerId);
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
            menus = systemMenuService.lambdaQuery()
                    .select(SystemMenu::getKey)
                    .list()
                    .stream().map(SystemMenu::getKey).collect(Collectors.toList());
            permissions = systemPermissionService.lambdaQuery()
                    .select(SystemPermission::getKey)
                    .list()
                    .stream().map(SystemPermission::getKey).collect(Collectors.toList());
        } else {
            // 普通账号
            SystemManagerService proxy = SpringUtil.getAopProxy(this);
            menus = proxy.getMenuKeyList(systemManager.getId());
            permissions = proxy.getPermissionKeyList(systemManager.getId());
        }

        // 地区权限
        switch (regionPermissionType) {
            case SystemRoleRegionPermissionType.ALL_REGION -> {
                regionIds = systemRegionService.lambdaQuery()
                        .select(SystemRegion::getId)
                        .list()
                        .stream()
                        .map(SystemRegion::getId)
                        .collect(Collectors.toList());
            }
            case SystemRoleRegionPermissionType.WITH_SUB_REGION -> {
                regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
            }
            case SystemRoleRegionPermissionType.CURR_REGION -> {
                if (systemManager.getRegionId() != null) {
                    regionIds = List.of(systemManager.getRegionId());
                } else {
                    regionIds = List.of("-1");
                }
            }
            default -> regionIds = List.of("-1");
        }

        return new LoginSystemManager(systemManager, systemRegion, regionPermissionType, menus, permissions, regionIds);
    }

    /**
     * 根据管理人员 ID, 刷新 redis 中的权限信息
     *
     * @param managerId 管理人员 ID
     */
    @Caching(evict = {
            @CacheEvict(value = "strix:system:manager:menu_by_mid", key = "#managerId"),
            @CacheEvict(value = "strix:system:manager:permission_by_mid", key = "#managerId")
    })
    public void refreshLoginInfoByManager(String managerId) {
        LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
        tokenSessionService.refreshManagerLoginInfo(managerId, loginSystemManager);
    }

    /**
     * 根据角色 ID, 刷新 redis 中的权限信息
     *
     * @param roleId 角色 ID
     */
    public void refreshLoginInfoByRole(String roleId) {
        refreshLoginInfoForManagers(getManagerIdListByRoleId(roleId));
    }

    /**
     * 根据角色 ID 列表, 刷新 redis 中的权限信息
     *
     * @param roleIdList 角色 ID 列表
     */
    public void refreshLoginInfoByRole(List<String> roleIdList) {
        List<String> managerIds = systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemManagerId)
                .in(SystemManagerRole::getSystemRoleId, roleIdList)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .collect(Collectors.toList());
        refreshLoginInfoForManagers(managerIds);
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

    /**
     * 批量刷新管理人员权限信息（通过 AOP 代理确保缓存正确淘汰）
     */
    private void refreshLoginInfoForManagers(List<String> managerIds) {
        if (CollectionUtils.isEmpty(managerIds)) {
            return;
        }
        SystemManagerService proxy = SpringUtil.getAopProxy(this);
        managerIds.forEach(proxy::refreshLoginInfoByManager);
    }

    /**
     * 刷新所有在线管理员的 LoginInfo
     * (用于菜单/权限全局变更时)
     */
    public void refreshLoginInfoForAllOnlineManagers() {
        Set<String> onlineManagerIds = tokenSessionService.getOnlineManagerIds();
        if (!onlineManagerIds.isEmpty()) {
            log.info("刷新所有在线管理员 LoginInfo, 在线人数: {}", onlineManagerIds.size());
            refreshLoginInfoForManagers(new ArrayList<>(onlineManagerIds));
        }
    }

    /**
     * 清除指定地区ID的管理人员地区权限
     *
     * @param regionIds 地区ID列表
     */
    public void clearRegionId(List<String> regionIds) {
        if (CollectionUtils.isEmpty(regionIds)) {
            return;
        }
        lambdaUpdate()
                .in(SystemManager::getRegionId, regionIds)
                .set(SystemManager::getRegionId, null)
                .update();
    }

    /**
     * 批量删除管理员及其关联数据
     *
     * @param managerIds 管理员 ID 列表
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void batchRemoveWithRelations(List<String> managerIds) {
        if (CollectionUtils.isEmpty(managerIds)) {
            return;
        }
        // 1. 批量删除角色关系
        systemManagerRoleService.lambdaUpdate()
                .in(SystemManagerRole::getSystemManagerId, managerIds)
                .remove();
        // 2. 批量删除管理员
        removeByIds(managerIds);
        // 3. 批量失效 Token
        managerIds.forEach(tokenSessionService::invalidateManagerSession);
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
