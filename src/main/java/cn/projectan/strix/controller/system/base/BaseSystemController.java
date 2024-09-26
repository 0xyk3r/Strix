package cn.projectan.strix.controller.system.base;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.dict.SystemRoleRegionPermissionType;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统管理端基础控制器
 *
 * @author ProjectAn
 * @date 2021/5/13 18:30
 */
public class BaseSystemController extends BaseController {

    private static final List<String> EMPTY_FILL_LIST = List.of("-1");

    protected SystemManager loginManager() {
        return SecurityUtils.getSystemManager();
    }

    /**
     * 获取当前登录账号的ID
     */
    protected String loginManagerId() {
        return SecurityUtils.getManagerId();
    }

    /**
     * 获取当前登录账号的地区ID
     */
    protected String loginManagerRegionId() {
        return SecurityUtils.getManagerRegionId();
    }

    /**
     * 检查当前账号是否为超级管理员
     */
    protected boolean isSuperManager() {
        return SecurityUtils.isSuperAdmin();
    }

    /**
     * 检查当前账号是否非超级管理员
     */
    protected boolean notSuperManager() {
        return !SecurityUtils.isSuperAdmin();
    }

    /**
     * 获取当前账号的地区权限
     */
    protected List<String> loginManagerRegionIdList() {
        List<String> loginSystemManagerRegionIdList = SecurityUtils.getManagerRegionIdList();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        return loginSystemManagerRegionIdList;
    }

    /**
     * 获取当前账号的地区权限, 排除当前地区
     */
    protected List<String> loginManagerRegionIdListExcludeCurrent() {
        List<String> loginSystemManagerRegionIdList = SecurityUtils.getManagerRegionIdList();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        loginSystemManagerRegionIdList.remove(loginManagerRegionId());
        return loginSystemManagerRegionIdList;
    }

    /**
     * 获取当前账号的地区权限 <br><br>
     * 返回 null 表示无需限制 <br>
     * 返回 [] 或 ["-1"] 表示无权限
     */
    protected static List<String> loginManagerRegionPermissions() {
        if (SecurityUtils.isSuperAdmin()) {
            return null;
        }
        byte regionPermissionType = SecurityUtils.getLoginInfo().getRegionPermissionType();
        switch (regionPermissionType) {
            case SystemRoleRegionPermissionType.ALL_REGION -> {
                return null;
            }
            case SystemRoleRegionPermissionType.WITH_SUB_REGION -> {
                return SecurityUtils.getManagerRegionIdList();
            }
            case SystemRoleRegionPermissionType.CURR_REGION -> {
                String regionId = SecurityUtils.getManagerRegionId();
                // 防止未配置地区ID的情况下, 查询越权
                if (!StringUtils.hasText(regionId)) {
                    regionId = "-1";
                }
                return List.of(regionId);
            }
            default -> throw new StrixException("无效的地区权限类型");
        }
    }

    /**
     * 检查当前账号是否具备访问指定地区的权限
     *
     * @param regionId 待检查的地区ID
     */
    protected static void checkLoginManagerRegionPermission(String regionId) {
        List<String> regionPermissions = loginManagerRegionPermissions();
        if (CollectionUtils.isEmpty(regionPermissions)) {
            return;
        }
        if (!regionPermissions.contains(regionId)) {
            throw new StrixException("无权限访问该地区数据");
        }
    }

}
