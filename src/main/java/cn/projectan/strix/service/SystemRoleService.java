package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRole;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.SortedSet;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
public interface SystemRoleService extends IService<SystemRole> {

    /**
     * 获取下拉框数据 （有缓存）
     * @return 下拉框数据
     */
    CommonSelectDataResp getSelectData();
    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 该角色具有的菜单权限
     */
    List<SystemMenu> getMenusByRoleId(String roleId);

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 该角色具有的菜单权限
     */
    List<SystemMenu> getMenusByRoleId(SortedSet<String> roleId);

    /**
     * 根据角色ID获取系统权限
     *
     * @param roleId 角色id
     * @return 该角色具有的系统权限
     */
    List<SystemPermission> getSystemPermissionByRoleId(String roleId);

    /**
     * 根据角色ID获取系统权限
     *
     * @param roleId 角色id
     * @return 该角色具有的系统权限
     */
    List<SystemPermission> getSystemPermissionByRoleId(SortedSet<String> roleId);

}
