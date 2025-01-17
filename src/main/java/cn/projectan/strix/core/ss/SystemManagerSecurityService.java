package cn.projectan.strix.core.ss;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Strix Security Service
 *
 * @author ProjectAn
 * @since 2023/2/25 16:40
 */
@Service("ss")
public class SystemManagerSecurityService {

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasPermission(String permission) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        Set<String> hasPermissionSet = SecurityUtils.getManagerPermissions();
        return !CollectionUtils.isEmpty(hasPermissionSet) && hasPermissionSet.contains(permission);
    }

    /**
     * 验证用户是否具有以下所有权限
     *
     * @param permissions 权限字符串数组
     * @return 用户是否具有以下所有权限
     */
    public boolean allPermission(String... permissions) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> permissionList = Arrays.asList(permissions);
        Set<String> hasPermissionSet = SecurityUtils.getManagerPermissions();
        return !CollectionUtils.isEmpty(hasPermissionSet) && hasPermissionSet.containsAll(permissionList);
    }

    /**
     * 验证用户是否具有以下任意一个权限
     *
     * @param permissions 权限字符串数组
     * @return 用户是否具有以下任意一个权限
     */
    public boolean anyPermission(String... permissions) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> permissionList = Arrays.asList(permissions);
        Set<String> hasPermissionSet = SecurityUtils.getManagerPermissions();
        return !CollectionUtils.isEmpty(hasPermissionSet) && hasPermissionSet.stream().anyMatch(permissionList::contains);
    }

    /**
     * 验证用户是否具备某菜单权限
     *
     * @param menu 菜单权限字符串
     * @return 用户是否具备某菜单权限
     * @deprecated 请使用 {@link #hasPermission(String)}
     */
    @Deprecated
    public boolean hasMenu(String menu) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (!StringUtils.hasText(menu)) {
            return false;
        }
        List<String> hasMenuKeys = Optional.ofNullable(SecurityUtils.getSystemManagerLoginInfo()).map(LoginSystemManager::getMenusKeys).orElse(null);
        return !CollectionUtils.isEmpty(hasMenuKeys) && hasMenuKeys.contains(menu);
    }

    /**
     * 验证用户是否具有以下任意一个菜单权限
     *
     * @param menus 菜单权限字符串数组
     * @return 用户是否具有以下任意一个菜单权限
     * @deprecated 请使用 {@link #anyPermission(String...)}
     */
    @Deprecated
    public boolean anyMenu(String... menus) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (menus == null || menus.length == 0) {
            return false;
        }
        List<String> hasMenuKeys = Optional.ofNullable(SecurityUtils.getSystemManagerLoginInfo()).map(LoginSystemManager::getMenusKeys).orElse(null);
        List<String> menuList = Arrays.asList(menus);
        return !CollectionUtils.isEmpty(hasMenuKeys) && hasMenuKeys.stream().anyMatch(menuList::contains);
    }

}
