package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.constant.SysLogOperType;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateMenuReq;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleResp;
import cn.projectan.strix.service.*;
import cn.projectan.strix.utils.KeysDiffHandler;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/1 16:35
 */
@Slf4j
@RestController
@RequestMapping("system/role")
public class SystemRoleController extends BaseSystemController {

    @Autowired
    private SystemRoleService systemRoleService;
    @Autowired
    private SystemRoleMenuService systemRoleMenuService;
    @Autowired
    private SystemManagerRoleService systemManagerRoleService;
    @Autowired
    private SystemRolePermissionService systemRolePermissionService;

    @Autowired
    private SystemMenuCache systemMenuCache;
    @Autowired
    private SystemPermissionCache systemPermissionCache;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @SysLog(operationGroup = "系统角色", operationName = "查询角色列表")
    public RetResult<SystemRoleListResp> getSystemRoleList() {
        QueryWrapper<SystemRole> systemRoleQueryWrapper = new QueryWrapper<>();
        systemRoleQueryWrapper.orderByAsc("create_time");
        List<SystemRole> systemRoleList = systemRoleService.list(systemRoleQueryWrapper);

        return RetMarker.makeSuccessRsp(new SystemRoleListResp(systemRoleList));
    }

    @GetMapping("{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @SysLog(operationGroup = "系统角色", operationName = "查询角色信息")
    public RetResult<SystemRoleResp> getSystemRole(@PathVariable String roleId) {
        Assert.notNull(roleId, "参数错误");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetMarker.makeSuccessRsp(new SystemRoleResp(systemRole.getId(), systemRole.getName(), menuItems, permissionList));
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:role:add')")
    @SysLog(operationGroup = "系统角色", operationName = "新增角色", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemRoleUpdateReq req) {
        Assert.notNull(req, "参数错误");

        SystemRole systemRole = new SystemRole(
                req.getName()
        );
        systemRole.setCreateBy(getLoginManagerId());
        systemRole.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(systemRole);

        Assert.isTrue(systemRoleService.save(systemRole), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @SysLog(operationGroup = "系统角色", operationName = "修改角色", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String roleId, @RequestBody @Validated(ValidationGroup.Update.class) SystemRoleUpdateReq req) {
        Assert.hasText(roleId, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        UpdateWrapper<SystemRole> updateWrapper = UpdateConditionBuilder.build(systemRole, req, getLoginManagerId());
        UniqueDetectionTool.check(systemRole);
        Assert.isTrue(systemRoleService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{roleId}/menu")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @SysLog(operationGroup = "系统角色", operationName = "修改角色菜单权限", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> updateMenu(@PathVariable String roleId, @RequestBody @Validated(ValidationGroup.Update.class) SystemRoleUpdateMenuReq req) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        // 修改角色的菜单权限
        QueryWrapper<SystemRoleMenu> systemRoleMenuQueryWrapper = new QueryWrapper<>();
        systemRoleMenuQueryWrapper.select("system_menu_id");
        systemRoleMenuQueryWrapper.eq("system_role_id", roleId);
        List<String> systemRoleMenuIds = systemRoleMenuService.listObjs(systemRoleMenuQueryWrapper, Object::toString);
        KeysDiffHandler.handle(systemRoleMenuIds, Arrays.asList(req.getMenuIds().split(",")), ((removeKeys, addKeys) -> {
            if (removeKeys.size() > 0) {
                QueryWrapper<SystemRoleMenu> removeQueryWrapper = new QueryWrapper<>();
                removeQueryWrapper.eq("system_role_id", roleId);
                removeQueryWrapper.in("system_menu_id", removeKeys);
                Assert.isTrue(systemRoleMenuService.remove(removeQueryWrapper), "移除该角色的菜单权限失败");
            }
            if (addKeys.size() > 0) {
                List<SystemRoleMenu> systemRoleMenuList = new ArrayList<>();
                addKeys.forEach(k -> {
                    SystemRoleMenu systemRoleMenu = new SystemRoleMenu();
                    systemRoleMenu.setSystemRoleId(roleId);
                    systemRoleMenu.setSystemMenuId(k);
                    systemRoleMenu.setCreateBy(getLoginManagerId());
                    systemRoleMenu.setUpdateBy(getLoginManagerId());
                    systemRoleMenuList.add(systemRoleMenu);
                });
                Assert.isTrue(systemRoleMenuService.saveBatch(systemRoleMenuList), "增加该角色的菜单权限失败");
            }
            // 刷新 redis 缓存
            systemMenuCache.updateRedisBySystemRoleId(roleId);
        }));
        // 修改角色的系统权限
        QueryWrapper<SystemRolePermission> systemRolePermissionQueryWrapper = new QueryWrapper<>();
        systemRolePermissionQueryWrapper.select("system_permission_id");
        systemRolePermissionQueryWrapper.eq("system_role_id", roleId);
        List<String> systemRolePermissionIds = systemRolePermissionService.listObjs(systemRolePermissionQueryWrapper, Object::toString);
        KeysDiffHandler.handle(systemRolePermissionIds, Arrays.asList(req.getPermissionIds().split(",")), ((removeKeys, addKeys) -> {
            if (removeKeys.size() > 0) {
                QueryWrapper<SystemRolePermission> removeQueryWrapper = new QueryWrapper<>();
                removeQueryWrapper.eq("system_role_id", roleId);
                removeQueryWrapper.in("system_permission_id", removeKeys);
                Assert.isTrue(systemRolePermissionService.remove(removeQueryWrapper), "移除该角色的菜单权限失败");
            }
            if (addKeys.size() > 0) {
                List<SystemRolePermission> systemRoleMenuList = new ArrayList<>();
                addKeys.forEach(k -> {
                    SystemRolePermission systemRolePermission = new SystemRolePermission();
                    systemRolePermission.setSystemRoleId(roleId);
                    systemRolePermission.setSystemPermissionId(k);
                    systemRolePermission.setCreateBy(getLoginManagerId());
                    systemRolePermission.setUpdateBy(getLoginManagerId());
                    systemRoleMenuList.add(systemRolePermission);
                });
                Assert.isTrue(systemRolePermissionService.saveBatch(systemRoleMenuList), "增加该角色的菜单权限失败");
            }
            // 刷新 redis 缓存
            systemPermissionCache.updateRedisBySystemRoleId(roleId);
        }));
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfo(
                systemManagerRoleService.listObjs(
                        new LambdaQueryWrapper<SystemManagerRole>()
                                .select(SystemManagerRole::getSystemManagerId)
                                .eq(SystemManagerRole::getSystemRoleId, roleId)
                ).stream().map(Object::toString).toList()
        );
        // 获取最新的权限信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetMarker.makeSuccessRsp(new SystemRoleResp(systemRole.getId(), systemRole.getName(), menuItems, permissionList));
    }

    @PostMapping("remove/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:remove')")
    @SysLog(operationGroup = "系统角色", operationName = "删除角色", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String roleId) {
        Assert.hasText(roleId, "参数错误");
        // TODO 改为lock字段
        Assert.isTrue(!"SuperManager".equalsIgnoreCase(roleId), "该角色不支持删除");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        systemRoleService.removeById(systemRole);

        // 删除管理人员和角色间关系
        QueryWrapper<SystemManagerRole> deleteManagerRoleRelationQueryWrapper = new QueryWrapper<>();
        deleteManagerRoleRelationQueryWrapper.eq("system_role_id", systemRole.getId());
        systemManagerRoleService.remove(deleteManagerRoleRelationQueryWrapper);
        // 删除角色和菜单间关系
        QueryWrapper<SystemRoleMenu> deleteRoleMenuRelationQueryWrapper = new QueryWrapper<>();
        deleteRoleMenuRelationQueryWrapper.eq("system_role_id", systemRole.getId());
        systemRoleMenuService.remove(deleteRoleMenuRelationQueryWrapper);
        // 删除角色和系统权限间关系
        QueryWrapper<SystemRolePermission> deleteRolePermissionRelationQueryWrapper = new QueryWrapper<>();
        deleteRolePermissionRelationQueryWrapper.eq("system_role_id", systemRole.getId());
        systemRolePermissionService.remove(deleteRolePermissionRelationQueryWrapper);

        return RetMarker.makeSuccessRsp();
    }

    /**
     * 移除角色的菜单权限
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    @PostMapping("remove/{roleId}/menu/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @SysLog(operationGroup = "系统角色", operationName = "移除角色的菜单权限", operationType = SysLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRoleMenu(@PathVariable String roleId, @PathVariable String menuId) {
        Assert.hasText(roleId, "参数错误");
        Assert.hasText(menuId, "参数错误");
        Assert.isTrue(!"SuperManager".equalsIgnoreCase(roleId), "该角色不支持进行该操作");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        // 查询该菜单和其子菜单的id 注意此处使用了ram缓存
        List<String> menuAndChildrenMenu = systemMenuCache.getIdListByParentMenu(menuId);

        QueryWrapper<SystemRoleMenu> systemRoleMenuQueryWrapper = new QueryWrapper<>();
        systemRoleMenuQueryWrapper.eq("system_role_id", roleId);
        systemRoleMenuQueryWrapper.in("system_menu_id", menuAndChildrenMenu);
        systemRoleMenuService.remove(systemRoleMenuQueryWrapper);
        // 刷新redis缓存
        systemMenuCache.updateRedisBySystemRoleId(roleId);

        // 返回移除后的最新关系信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        // 需要删除
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetMarker.makeSuccessRsp(new SystemRoleResp(systemRole.getId(), systemRole.getName(), menuItems, permissionList));
    }

    /**
     * 移除角色的系统权限
     *
     * @param roleId       角色id
     * @param permissionId 系统权限id
     */
    @PostMapping("remove/{roleId}/permission/{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @SysLog(operationGroup = "系统角色", operationName = "移除角色的系统权限", operationType = SysLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRolePermission(@PathVariable String roleId, @PathVariable String permissionId) {
        Assert.hasText(roleId, "参数错误");
        Assert.hasText(permissionId, "参数错误");
        Assert.isTrue(!"SuperManager".equalsIgnoreCase(roleId), "该角色不支持进行该操作");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        QueryWrapper<SystemRolePermission> systemRolePermissionQueryWrapper = new QueryWrapper<>();
        systemRolePermissionQueryWrapper.eq("system_role_id", roleId);
        systemRolePermissionQueryWrapper.eq("system_permission_id", permissionId);
        systemRolePermissionService.remove(systemRolePermissionQueryWrapper);
        // 刷新redis缓存
        systemPermissionCache.updateRedisBySystemRoleId(roleId);

        // 返回移除后的最新关系信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetMarker.makeSuccessRsp(new SystemRoleResp(systemRole.getId(), systemRole.getName(), menuItems, permissionList));
    }

    @GetMapping("select")
    public RetResult<CommonSelectDataResp> getSystemRoleSelectList() {
        return RetMarker.makeSuccessRsp(systemRoleService.getSelectData());
    }

}
