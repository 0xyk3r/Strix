package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.SystemManagerMapper;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.dict.SystemManagerType;
import cn.projectan.strix.service.*;
import cn.projectan.strix.utils.RedisUtil;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    @Cacheable(value = "strix:system:manager:menu_by_mid", key = "#systemManagerId")
    @Override
    public List<String> getMenuKeyList(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

        return systemRoleService.getMenusByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemMenu::getKey)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "strix:system:manager:permission_by_mid", key = "#systemManagerId")
    @Override
    public List<String> getPermissionKeyList(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

        return systemRoleService.getSystemPermissionByRoleId(new TreeSet<>(systemManagerRoleIdList))
                .stream()
                .map(SystemPermission::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public LoginSystemManager getLoginInfo(String systemManagerId) {
        SystemManagerServiceImpl proxy = SpringUtil.getAopProxy(this);

        SystemManager systemManager = proxy.getById(systemManagerId);

        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

        byte regionPermissionType = 0;
        if (!CollectionUtils.isEmpty(systemManagerRoleIdList)) {
            QueryWrapper<SystemRole> systemRoleQueryWrapper = new QueryWrapper<>();
            systemRoleQueryWrapper.select("region_permission_type");
            systemRoleQueryWrapper.in("id", systemManagerRoleIdList);
            systemRoleQueryWrapper.orderByAsc("region_permission_type");
            systemRoleQueryWrapper.last("limit 1");
            regionPermissionType = Optional.ofNullable(systemRoleService.getOne(systemRoleQueryWrapper))
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
        return new LoginSystemManager(systemManager, regionPermissionType, menus, permissions, regionIds);
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
        systemManagerRoleService.listObjs(
                        new LambdaQueryWrapper<SystemManagerRole>()
                                .select(SystemManagerRole::getSystemManagerId)
                                .eq(SystemManagerRole::getSystemRoleId, roleId)
                ).stream()
                .map(Object::toString)
                .forEach(managerId -> {
                    Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
                    if (existToken != null) {
                        LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
                        redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
                    }
                });
    }

    @Override
    public void refreshLoginInfoByRole(List<String> roleIdList) {
        systemManagerRoleService.listObjs(
                        new LambdaQueryWrapper<SystemManagerRole>()
                                .select(SystemManagerRole::getSystemManagerId)
                                .in(SystemManagerRole::getSystemRoleId, roleIdList)
                ).stream()
                .map(Object::toString)
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
        List<String> roleIdList = systemRoleMenuService.listObjs(
                        new LambdaQueryWrapper<SystemRoleMenu>()
                                .select(SystemRoleMenu::getSystemRoleId)
                                .eq(SystemRoleMenu::getSystemMenuId, menuId)
                ).stream()
                .map(Object::toString).collect(Collectors.toList());

        if (!roleIdList.isEmpty()) {
            refreshLoginInfoByRole(roleIdList);
        }
    }

    @Override
    public void refreshLoginInfoByPermission(String permissionId) {
        List<String> roleIdList = systemRolePermissionService.listObjs(
                        new LambdaQueryWrapper<SystemRolePermission>()
                                .select(SystemRolePermission::getSystemRoleId)
                                .eq(SystemRolePermission::getSystemPermissionId, permissionId)
                ).stream()
                .map(Object::toString).collect(Collectors.toList());

        if (!roleIdList.isEmpty()) {
            refreshLoginInfoByRole(roleIdList);
        }
    }
}
