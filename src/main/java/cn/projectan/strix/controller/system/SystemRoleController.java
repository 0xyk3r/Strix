package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.constant.BuiltinConstant;
import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateMenuReq;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleResp;
import cn.projectan.strix.service.*;
import cn.projectan.strix.utils.KeyDiffUtil;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色
 *
 * @author ProjectAn
 * @since 2021/7/1 16:35
 */
@Slf4j
@RestController
@RequestMapping("system/role")
@RequiredArgsConstructor
public class SystemRoleController extends BaseSystemController {

    private final SystemRoleService systemRoleService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final SystemMenuCache systemMenuCache;
    private final SystemPermissionCache systemPermissionCache;

    /**
     * 查询角色列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @StrixLog(operationGroup = "系统角色", operationName = "查询角色列表")
    public RetResult<SystemRoleListResp> getSystemRoleList() {
        List<SystemRole> systemRoleList = systemRoleService.lambdaQuery()
                .orderByAsc(SystemRole::getCreateTime)
                .list();

        return RetBuilder.success(new SystemRoleListResp(systemRoleList));
    }

    /**
     * 查询角色信息
     */
    @GetMapping("{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @StrixLog(operationGroup = "系统角色", operationName = "查询角色信息")
    public RetResult<SystemRoleResp> getSystemRole(@PathVariable String roleId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");

        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetBuilder.success(new SystemRoleResp(systemRole.getId(), systemRole.getName(), systemRole.getRegionPermissionType(), menuItems, permissionList));
    }

    /**
     * 新增角色
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:role:add')")
    @StrixLog(operationGroup = "系统角色", operationName = "新增角色", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemRoleUpdateReq req) {
        Assert.notNull(req, "参数错误");

        SystemRole systemRole = new SystemRole(
                req.getName(),
                req.getRegionPermissionType(),
                BuiltinConstant.NO
        );

        UniqueDetectionTool.check(systemRole);

        Assert.isTrue(systemRoleService.save(systemRole), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改角色
     */
    @PostMapping("update/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @StrixLog(operationGroup = "系统角色", operationName = "修改角色", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String roleId, @RequestBody @Validated(UpdateGroup.class) SystemRoleUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemRole.getBuiltin(), "系统内置角色不支持修改");

        LambdaUpdateWrapper<SystemRole> updateWrapper = UpdateConditionBuilder.build(systemRole, req);
        UniqueDetectionTool.check(systemRole);
        Assert.isTrue(systemRoleService.update(updateWrapper), "保存失败");

        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByRole(roleId);

        return RetBuilder.success();
    }

    /**
     * 修改角色的菜单权限
     */
    @PostMapping("update/{roleId}/menu")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @StrixLog(operationGroup = "系统角色", operationName = "修改角色菜单权限", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> updateMenu(@PathVariable String roleId, @RequestBody @Validated(UpdateGroup.class) SystemRoleUpdateMenuReq req) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemRole.getBuiltin(), "系统内置角色不支持修改");

        // 修改角色的菜单权限
        List<String> systemRoleMenuIds = systemRoleMenuService.lambdaQuery()
                .select(SystemRoleMenu::getSystemMenuId)
                .eq(SystemRoleMenu::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRoleMenu::getSystemMenuId)
                .collect(Collectors.toList());

        KeyDiffUtil.handle(systemRoleMenuIds, Arrays.asList(req.getMenuIds().split(",")),
                (removeKeys) -> {
                    Assert.isTrue(
                            systemRoleMenuService.lambdaUpdate()
                                    .eq(SystemRoleMenu::getSystemRoleId, roleId)
                                    .in(SystemRoleMenu::getSystemMenuId, removeKeys)
                                    .remove(),
                            "移除该角色的菜单权限失败");
                },
                (addKeys) -> {
                    List<SystemRoleMenu> systemRoleMenuList = addKeys.stream()
                            .map(k -> new SystemRoleMenu(roleId, k))
                            .collect(Collectors.toList());
                    Assert.isTrue(systemRoleMenuService.saveBatch(systemRoleMenuList), "增加该角色的菜单权限失败");
                },
                () -> {
                    // 刷新 redis 缓存
                    systemMenuCache.updateRedisBySystemRoleId(roleId);
                }
        );
        // 修改角色的系统权限
        List<String> systemRolePermissionIds = systemRolePermissionService.lambdaQuery()
                .select(SystemRolePermission::getSystemPermissionId)
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRolePermission::getSystemPermissionId)
                .collect(Collectors.toList());

        KeyDiffUtil.handle(systemRolePermissionIds, Arrays.asList(req.getPermissionIds().split(",")),
                (removeKeys) -> {
                    Assert.isTrue(
                            systemRolePermissionService.lambdaUpdate()
                                    .eq(SystemRolePermission::getSystemRoleId, roleId)
                                    .in(SystemRolePermission::getSystemPermissionId, removeKeys)
                                    .remove(),
                            "移除该角色的菜单权限失败");
                },
                (addKeys) -> {
                    List<SystemRolePermission> systemRolePermissionList = addKeys.stream()
                            .map(k -> new SystemRolePermission(roleId, k))
                            .collect(Collectors.toList());
                    Assert.isTrue(systemRolePermissionService.saveBatch(systemRolePermissionList), "增加该角色的菜单权限失败");
                },
                () -> {
                    // 刷新 redis 缓存
                    systemPermissionCache.updateRedisBySystemRoleId(roleId);
                }
        );

        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 获取最新的权限信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetBuilder.success(new SystemRoleResp(systemRole.getId(), systemRole.getName(), systemRole.getRegionPermissionType(), menuItems, permissionList));
    }

    /**
     * 删除角色
     */
    @PostMapping("remove/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:remove')")
    @StrixLog(operationGroup = "系统角色", operationName = "删除角色", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String roleId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemRole.getBuiltin(), "系统内置角色不支持删除");

        systemRoleService.removeById(systemRole);

        // 删除管理人员和角色间关系
        systemManagerRoleService.lambdaUpdate()
                .eq(SystemManagerRole::getSystemRoleId, systemRole.getId())
                .remove();
        // 删除角色和菜单间关系
        systemRoleMenuService.lambdaUpdate()
                .eq(SystemRoleMenu::getSystemRoleId, systemRole.getId())
                .remove();
        // 删除角色和系统权限间关系
        systemRolePermissionService.lambdaUpdate()
                .eq(SystemRolePermission::getSystemRoleId, systemRole.getId())
                .remove();

        return RetBuilder.success();
    }

    /**
     * 移除角色的菜单权限
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    @PostMapping("remove/{roleId}/menu/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @StrixLog(operationGroup = "系统角色", operationName = "移除角色的菜单权限", operationType = SysLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRoleMenu(@PathVariable String roleId, @PathVariable String menuId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemRole.getBuiltin(), "系统内置角色不支持修改");

        // 查询该菜单和其子菜单的id 注意此处使用了ram缓存
        List<String> menuAndChildrenMenu = systemMenuCache.getIdListByParentMenu(menuId);

        systemRoleMenuService.lambdaUpdate()
                .eq(SystemRoleMenu::getSystemRoleId, roleId)
                .in(SystemRoleMenu::getSystemMenuId, menuAndChildrenMenu)
                .remove();

        // 刷新redis缓存
        systemMenuCache.updateRedisBySystemRoleId(roleId);
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 返回移除后的最新关系信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        // 需要删除
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetBuilder.success(new SystemRoleResp(systemRole.getId(), systemRole.getName(), systemRole.getRegionPermissionType(), menuItems, permissionList));
    }

    /**
     * 移除角色的系统权限
     *
     * @param roleId       角色id
     * @param permissionId 系统权限id
     */
    @PostMapping("remove/{roleId}/permission/{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @StrixLog(operationGroup = "系统角色", operationName = "移除角色的系统权限", operationType = SysLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRolePermission(@PathVariable String roleId, @PathVariable String permissionId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, "系统角色信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemRole.getBuiltin(), "系统内置角色不支持修改");

        systemRolePermissionService.lambdaUpdate()
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .eq(SystemRolePermission::getSystemPermissionId, permissionId)
                .remove();

        // 刷新redis缓存
        systemPermissionCache.updateRedisBySystemRoleId(roleId);
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 返回移除后的最新关系信息
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return RetBuilder.success(new SystemRoleResp(systemRole.getId(), systemRole.getName(), systemRole.getRegionPermissionType(), menuItems, permissionList));
    }

    /**
     * 获取系统角色下拉列表
     */
    @GetMapping("select")
    public RetResult<CommonSelectDataResp> getSystemRoleSelectList() {
        return RetBuilder.success(systemRoleService.getSelectData());
    }

}
