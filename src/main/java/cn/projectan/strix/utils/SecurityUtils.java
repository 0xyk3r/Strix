package cn.projectan.strix.utils;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemPermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/2/25 16:42
 */
public class SecurityUtils {

    /**
     * 用户ID
     **/
    public static String getUserId() {
        try {
            return getSystemManager().getId();
        } catch (Exception e) {
            throw new StrixException("获取登录用户信息异常");
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

    public static List<SystemPermission> getSystemPermission() {
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
            throw new StrixException("获取登录用户信息异常");
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
        return getSystemManager().getManagerType() == SystemManagerType.SUPER_ACCOUNT;
    }

}
