package cn.projectan.strix.utils;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.dict.SystemManagerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 安炯奕
 * @date 2023/2/25 16:42
 */
@Slf4j
public class SecurityUtils {

    /**
     * 用户ID
     **/
    public static String getUserId() {
        try {
            return getSystemManager().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取所属地区ID
     **/
    public static String getRegionId() {
        try {
            return getSystemManager().getRegionId();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    /**
     * 获取用户名称
     **/
    public static String getUsername() {
        try {
            return getSystemManager().getNickname();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    public static List<String> gerRegionIds() {
        try {
            return getLoginSystemManager().getRegionIds();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    public static Collection<? extends GrantedAuthority> getAuthorities() {
        try {
            return getLoginSystemManager().getAuthorities();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    public static List<SystemMenu> getSystemMenus() {
        try {
            return getLoginSystemManager().getMenus();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    /**
     * 获取用户权限列表 （合并了菜单权限和系统权限）
     *
     * @return 权限列表
     */
    public static Set<String> getHasPermissionSet() {
        try {
            Set<String> permissionSet = new HashSet<>();
            permissionSet.addAll(getLoginSystemManager().getMenus().stream().map(SystemMenu::getKey).toList());
            permissionSet.addAll(getLoginSystemManager().getPermissions().stream().map(SystemPermission::getKey).toList());
            return permissionSet;
        } catch (Exception e) {
            throw new StrixException("获取用户权限信息异常");
        }
    }

    public static List<SystemPermission> getSystemPermissions() {
        try {
            return getLoginSystemManager().getPermissions();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    /**
     * 获取用户
     **/
    public static LoginSystemManager getLoginSystemManager() {
        try {
            return (LoginSystemManager) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
        }
    }

    public static SystemManager getSystemManager() {
        try {
            return getLoginSystemManager().getSystemManager();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 是否为超级管理员
     *
     * @return 结果
     */
    public static boolean isSuperAdmin() {
        // TODO 这里理论上应该是判断角色是否为超级管理员，但是目前判断的是账号的数据权限。需要修改。
        return getSystemManager() != null && getSystemManager().getType() == SystemManagerType.SUPER_ACCOUNT;
    }

}
