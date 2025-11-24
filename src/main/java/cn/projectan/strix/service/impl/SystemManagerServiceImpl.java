package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.SystemManagerMapper;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.dict.SystemManagerType;
import cn.projectan.strix.service.*;
import cn.projectan.strix.util.RedisUtil;
import cn.projectan.strix.util.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
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
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Service
@RequiredArgsConstructor
public class SystemManagerServiceImpl extends ServiceImpl<SystemManagerMapper, SystemManager> implements SystemManagerService {

    private final SystemRoleService systemRoleService;
    private final SystemMenuService systemMenuService;
    private final SystemRegionService systemRegionService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final RedisUtil redisUtil;

    @Override
    public List<String> getManagerIdListByRoleId(String roleId) {
        return systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemManagerId)
                .eq(SystemManagerRole::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleIdListByManagerId(String managerId) {
        return systemManagerRoleService.lambdaQuery()
                .select(SystemManagerRole::getSystemRoleId)
                .eq(SystemManagerRole::getSystemManagerId, managerId)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemRoleId)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "strix:system:manager:menu_by_mid", key = "#systemManagerId")
    @Override
    public List<String> getMenuKeyList(String systemManagerId) {
        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(systemManagerId);

        return systemRoleService.getMenusByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemMenu::getKey)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "strix:system:manager:permission_by_mid", key = "#systemManagerId")
    @Override
    public List<String> getPermissionKeyList(String systemManagerId) {
        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(systemManagerId);

        return systemRoleService.getSystemPermissionByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemPermission::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public LoginSystemManager getLoginInfo(String systemManagerId) {
        SystemManagerServiceImpl proxy = SpringUtil.getAopProxy(this);

        SystemManager systemManager = proxy.getById(systemManagerId);
        SystemRegion systemRegion = null;
        if (StringUtils.hasText(systemManager.getRegionId())) {
            systemRegion = systemRegionService.getById(systemManager.getRegionId());
        }

        List<String> systemManagerRoleIdList = getRoleIdListByManagerId(systemManagerId);

        // 获取地区权限类型
        byte regionPermissionType = 0;
        if (!CollectionUtils.isEmpty(systemManagerRoleIdList)) {
            regionPermissionType = Optional.ofNullable(
                            systemRoleService.lambdaQuery()
                                    .select(SystemRole::getRegionPermissionType)
                                    .in(SystemRole::getId, systemManagerRoleIdList)
                                    .orderByAsc(SystemRole::getRegionPermissionType)
                                    .last("limit 1")
                                    .one())
                    .map(SystemRole::getRegionPermissionType)
                    .orElse((byte) 0);
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
            menus = proxy.getMenuKeyList(systemManager.getId());
            permissions = proxy.getPermissionKeyList(systemManager.getId());
            if (StringUtils.hasText(systemManager.getRegionId())) {
                regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
            }
        }
        return new LoginSystemManager(systemManager, systemRegion, regionPermissionType, menus, permissions, regionIds);
    }

    @Override
    public void refreshLoginInfoByManager(String systemManagerId) {
        Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManagerId);
        if (existToken != null) {
            LoginSystemManager loginSystemManager = this.getLoginInfo(systemManagerId);
            redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
        }
    }

    @Override
    public void refreshLoginInfoByRole(String roleId) {
        getManagerIdListByRoleId(roleId).forEach(managerId -> {
            Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
            if (existToken != null) {
                LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
                redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
            }
        });
    }

    @Override
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

    @Override
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

    @Override
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
