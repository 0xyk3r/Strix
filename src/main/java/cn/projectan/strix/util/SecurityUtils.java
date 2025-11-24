package cn.projectan.strix.util;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.constant.OperatorType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.dict.SystemManagerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 安全工具类
 *
 * @author ProjectAn
 * @since 2023/2/25 16:42
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取操作者类型
     *
     * @return 操作者类型
     * @see OperatorType 操作者类型
     */
    public static short getOperatorType() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return OperatorType.NONE;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginSystemManager) {
            return OperatorType.SYSTEM_MANAGER;
        } else if (principal instanceof LoginSystemUser) {
            return OperatorType.SYSTEM_USER;
        } else {
            return OperatorType.NONE;
        }
    }

    /**
     * 获取操作者ID
     *
     * @return 操作者ID
     */
    public static String getOperatorId() {
        short operatorType = getOperatorType();
        return switch (operatorType) {
            case OperatorType.SYSTEM_MANAGER ->
                    Optional.ofNullable(getSystemManagerLoginInfo()).map(LoginSystemManager::getSystemManager).map(SystemManager::getId).orElse(null);
            case OperatorType.SYSTEM_USER ->
                    Optional.ofNullable(getSystemUserLoginInfo()).map(LoginSystemUser::getSystemUser).map(SystemUser::getId).orElse(null);
            default -> null;
        };
    }

    public static SystemRegion getRegion() {
        return switch (getOperatorType()) {
            case OperatorType.SYSTEM_MANAGER ->
                    Optional.ofNullable(getSystemManagerLoginInfo()).map(LoginSystemManager::getSystemRegion).orElse(null);
            default -> null;
        };
    }

    public static List<String> getRegionIdList() {
        return switch (getOperatorType()) {
            case OperatorType.SYSTEM_MANAGER ->
                    Optional.ofNullable(getSystemManagerLoginInfo()).map(LoginSystemManager::getRegionIds).orElse(null);
            default -> List.of();
        };
    }

    /**
     * 获取登录的管理人员的登录信息
     *
     * @return 登录的管理人员的登录信息
     */
    public static LoginSystemManager getSystemManagerLoginInfo() {
        if (getOperatorType() != OperatorType.SYSTEM_MANAGER) {
            return null;
        }
        return Optional.ofNullable(getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(LoginSystemManager.class::isInstance)
                .map(LoginSystemManager.class::cast)
                .orElse(null);
    }

    /**
     * 获取登录的系统管理人员的信息
     *
     * @return 登录的系统管理人员的信息
     */
    public static SystemManager getSystemManager() {
        return Optional.ofNullable(getSystemManagerLoginInfo()).map(LoginSystemManager::getSystemManager).orElse(null);
    }

    /**
     * 获取登录的用户的登录信息
     *
     * @return 登录的用户的登录信息
     */
    public static LoginSystemUser getSystemUserLoginInfo() {
        return Optional.ofNullable(getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(LoginSystemUser.class::isInstance)
                .map(LoginSystemUser.class::cast)
                .orElse(null);
    }

    /**
     * 获取登录的系统用户的信息
     *
     * @return 登录的系统用户的信息
     */
    public static SystemUser getSystemUser() {
        return Optional.ofNullable(getSystemUserLoginInfo()).map(LoginSystemUser::getSystemUser).orElse(null);
    }

    /**
     * 获取登录的系统管理人员的权限列表 （合并了菜单权限和系统权限）
     *
     * @return 登录的系统管理人员的权限列表
     */
    public static Set<String> getManagerPermissions() {
        LoginSystemManager systemManagerLoginInfo = getSystemManagerLoginInfo();
        if (systemManagerLoginInfo == null) {
            return Set.of();
        }
        Set<String> permissionSet = new HashSet<>();
        permissionSet.addAll(systemManagerLoginInfo.getMenusKeys());
        permissionSet.addAll(systemManagerLoginInfo.getPermissionKeys());
        return permissionSet;
    }

    /**
     * 判断登录的系统管理人员是否为超级管理员
     *
     * @return 登录的系统管理人员是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return getSystemManager() != null && getSystemManager().getType() == SystemManagerType.SUPER_ACCOUNT;
    }

    /**
     * 获取登录用户 Authentication
     *
     * @return 登录用户 Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
