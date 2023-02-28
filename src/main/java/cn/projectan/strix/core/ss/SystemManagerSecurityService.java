package cn.projectan.strix.core.ss;

import cn.projectan.strix.model.constant.SystemPermissionType;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.utils.ListDiffUtil;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2023/2/25 16:40
 */
@Service("ss")
public class SystemManagerSecurityService {

    public static final String RW_SUFFIX = "/RW";

    public static final String R_SUFFIX = "/R";

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasWrite(final String permission) {
        if (!StringUtils.hasText(permission)) return false;
        if (SecurityUtils.isSuperAdmin()) return true;

        List<SystemPermission> hasPermissions = SecurityUtils.getSystemPermission();
        if (CollectionUtils.isEmpty(hasPermissions)) return false;

        return hasPermissions.stream()
                .anyMatch(p -> p.getPermissionType().equals(SystemPermissionType.READ_WRITE) && p.getPermissionKey().equals(permission));
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasRead(final String permission) {
        if (!StringUtils.hasText(permission)) return false;
        if (SecurityUtils.isSuperAdmin()) return true;

        List<SystemPermission> hasPermissions = SecurityUtils.getSystemPermission();
        if (CollectionUtils.isEmpty(hasPermissions)) return false;

        return hasPermissions.stream()
                .anyMatch(p -> p.getPermissionKey().equals(permission));
    }

    /**
     * 验证用户是否具有以下任意一个权限
     *
     * @param permissions 权限字符串
     * @return 用户是否具有以下任意一个权限
     */
    public boolean anyWrite(String... permissions) {
        if (permissions == null || permissions.length == 0) return false;
        if (SecurityUtils.isSuperAdmin()) return true;

        List<SystemPermission> hasPermissions = SecurityUtils.getSystemPermission();
        if (CollectionUtils.isEmpty(hasPermissions)) return false;

        List<String> permissionList = Arrays.asList(permissions);
        List<String> hasPermissionList = hasPermissions.stream()
                .filter(p -> p.getPermissionType().equals(SystemPermissionType.READ_WRITE))
                .map(SystemPermission::getPermissionKey)
                .collect(Collectors.toList());

        List<String> diffList = ListDiffUtil.subList(permissionList, hasPermissionList);
        return diffList.size() == 0;
    }

    /**
     * 验证用户是否具有以下任意一个权限
     *
     * @param permissions 权限字符串
     * @return 用户是否具有以下任意一个权限
     */
    public boolean anyRead(String... permissions) {
        if (permissions == null || permissions.length == 0) return false;
        if (SecurityUtils.isSuperAdmin()) return true;

        List<SystemPermission> hasPermissions = SecurityUtils.getSystemPermission();
        if (CollectionUtils.isEmpty(hasPermissions)) return false;

        List<String> permissionList = Arrays.asList(permissions);
        List<String> hasPermissionList = hasPermissions.stream()
                .map(SystemPermission::getPermissionKey)
                .collect(Collectors.toList());

        List<String> diffList = ListDiffUtil.subList(permissionList, hasPermissionList);
        return diffList.size() == 0;
    }

}
