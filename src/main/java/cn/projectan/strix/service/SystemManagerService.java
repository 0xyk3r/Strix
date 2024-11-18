package cn.projectan.strix.service;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
public interface SystemManagerService extends IDMapperService<SystemManager> {

    /**
     * 根据角色ID获取人员ID列表
     */
    List<String> getManagerIdListByRoleId(String roleId);

    /**
     * 根据人员ID获取角色ID列表
     */
    List<String> getRoleIdListByManagerId(String managerId);

    /**
     * 获取管理用户的所有菜单权限
     */
    List<String> getMenuKeyList(String systemManagerId);

    /**
     * 获取管理用户的所有系统权限
     */
    List<String> getPermissionKeyList(String systemManagerId);

    /**
     * 登陆时获取用户完整权限信息
     *
     * @param systemManagerId 管理用户ID
     * @return LoginSystemManager
     */
    LoginSystemManager getLoginInfo(String systemManagerId);

    /**
     * 根据用户 ID, 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfoByManager(String systemManagerId);

    /**
     * 根据角色 ID, 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfoByRole(String roleId);

    /**
     * 根据角色 ID, 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfoByRole(List<String> roleIdList);

    /**
     * 根据菜单 ID, 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfoByMenu(String menuId);

    /**
     * 根据权限 ID, 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfoByPermission(String permissionId);

}
