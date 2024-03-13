package cn.projectan.strix.utils;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.dict.SystemManagerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

/**
 * @author ProjectAn
 * @date 2023/2/25 16:42
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取登录用户ID
     *
     * @return 登录用户ID
     */
    public static String getManagerId() {
        return Optional.ofNullable(getSystemManager()).map(SystemManager::getId).orElse(null);
    }

    /**
     * 获取登录用户所属区域ID
     *
     * @return 登录用户所属区域ID
     */
    public static String getManagerRegionId() {
        return Optional.ofNullable(getSystemManager()).map(SystemManager::getRegionId).orElse(null);
    }

    /**
     * 获取登录用户昵称
     *
     * @return 登录用户昵称
     */
    public static String getManagerName() {
        return Optional.ofNullable(getSystemManager()).map(SystemManager::getNickname).orElse(null);
    }

    /**
     * 获取登录用户具有的区域权限的ID列表
     *
     * @return 登录用户具有的区域权限的ID列表
     */
    public static List<String> getManagerRegionIdList() {
        return Optional.ofNullable(getLoginInfo()).map(LoginSystemManager::getRegionIds).orElse(null);
    }

    /**
     * 获取登录用户具有的菜单权限列表
     *
     * @return 登录用户具有的菜单权限列表
     */
    public static List<SystemMenu> getManagerMenuPermissions() {
        return Optional.ofNullable(getLoginInfo()).map(LoginSystemManager::getMenus).orElse(null);
    }

    /**
     * 获取登录用户权限列表 （合并了菜单权限和系统权限）
     *
     * @return 登录用户权限列表
     */
    public static Set<String> getManagerAllPermissions() {
        try {
            Set<String> permissionSet = new HashSet<>();
            permissionSet.addAll(getLoginInfo().getMenus().stream().map(SystemMenu::getKey).toList());
            permissionSet.addAll(getLoginInfo().getPermissions().stream().map(SystemPermission::getKey).toList());
            return permissionSet;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取登录用户信息
     *
     * @return 登录用户信息
     */
    public static SystemManager getSystemManager() {
        return Optional.ofNullable(getLoginInfo()).map(LoginSystemManager::getSystemManager).orElse(null);
    }

    /**
     * 获取登录用户信息
     *
     * @return 登录用户信息
     */
    public static LoginSystemManager getLoginInfo() {
        return Optional.ofNullable(getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(LoginSystemManager.class::isInstance)
                .map(LoginSystemManager.class::cast)
                .orElse(null);
    }

    /**
     * 获取登录用户 Authorities
     *
     * @return 登录用户 Authorities
     */
    public static Collection<? extends GrantedAuthority> getAuthorities() {
        return Optional.ofNullable(getLoginInfo()).map(LoginSystemManager::getAuthorities).orElse(null);
    }

    /**
     * 获取登录用户 Authentication
     *
     * @return 登录用户 Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断登录用户是否为系统用户
     *
     * @return 登录用户是否为系统用户
     */
    public static boolean isSystemUser() {
        Object principal = SecurityUtils.getAuthentication().getPrincipal();
        return principal instanceof SystemUser;
    }

    /**
     * 判断登录用户是否为系统管理员
     *
     * @return 登录用户是否为系统管理员
     */
    public static boolean isSystemManager() {
        Object principal = SecurityUtils.getAuthentication().getPrincipal();
        return principal instanceof LoginSystemManager;
    }

    /**
     * 判断登录用户是否为超级管理员
     *
     * @return 登录用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        // TODO 这里理论上应该是判断角色是否为超级管理员，但是目前判断的是账号的数据权限。需要修改。
        return getSystemManager() != null && getSystemManager().getType() == SystemManagerType.SUPER_ACCOUNT;
    }

}
