package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateMenuReq;
import cn.projectan.strix.model.request.system.role.SystemRoleUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.common.CommonTransferDataResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleListResp;
import cn.projectan.strix.model.response.system.role.SystemRoleResp;
import cn.projectan.strix.service.system.*;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统 - 角色管理")
public class SystemRoleController extends BaseSystemController {

    private final SystemRoleService systemRoleService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final SystemManagerService systemManagerService;
    private final SystemMenuCache systemMenuCache;
    private final SystemPermissionCache systemPermissionCache;

    /**
     * 查询角色列表
     */
    @Operation(summary = "角色列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @StrixLog(operationGroup = "系统角色", operationName = "查询角色列表")
    public RetResult<SystemRoleListResp> getSystemRoleList() {
        List<SystemRole> systemRoleList = systemRoleService.listAll();

        return RetBuilder.success(new SystemRoleListResp(systemRoleList));
    }

    /**
     * 查询角色信息
     */
    @Operation(summary = "角色详情")
    @GetMapping("{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role')")
    @StrixLog(operationGroup = "系统角色", operationName = "查询角色信息")
    public RetResult<SystemRoleResp> getSystemRole(@Parameter(description = "角色 ID") @PathVariable String roleId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));

        return RetBuilder.success(buildRoleResp(systemRole, roleId));
    }

    /**
     * 新增角色
     */
    @Operation(summary = "新增角色")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:role:add')")
    @StrixLog(operationGroup = "系统角色", operationName = "新增角色", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemRoleUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));

        SystemRole systemRole = new SystemRole(
                req.getName(),
                req.getRegionPermissionType(),
                CommonFlag.NO
        );

        UniqueChecker.check(systemRole);

        Assert.isTrue(systemRoleService.save(systemRole), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改角色
     */
    @Operation(summary = "编辑角色")
    @PostMapping("update/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @StrixLog(operationGroup = "系统角色", operationName = "修改角色", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "角色 ID") @PathVariable String roleId, @RequestBody @Validated(UpdateGroup.class) SystemRoleUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));
        Assert.isTrue(systemRole.getBuiltin() == CommonFlag.NO, I18nUtil.get("assert.role.builtinNoModify"));

        LambdaUpdateWrapper<SystemRole> updateWrapper = UpdateBuilder.build(systemRole, req);
        UniqueChecker.check(systemRole);
        Assert.isTrue(systemRoleService.update(updateWrapper), "保存失败");

        // 刷新 redis 中的登录用户信息
        systemManagerService.refreshLoginInfoByRole(roleId);

        return RetBuilder.success();
    }

    /**
     * 修改角色的菜单权限
     */
    @Operation(summary = "更新角色菜单")
    @PostMapping("update/{roleId}/menu")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    @StrixLog(operationGroup = "系统角色", operationName = "修改角色菜单权限", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> updateMenu(@Parameter(description = "角色 ID") @PathVariable String roleId, @RequestBody @Validated(UpdateGroup.class) SystemRoleUpdateMenuReq req) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));
        Assert.isTrue(systemRole.getBuiltin() == CommonFlag.NO, I18nUtil.get("assert.role.builtinNoModify"));

        // 修改角色的菜单权限
        List<String> systemRoleMenuIds = systemRoleMenuService.listMenuIdsByRoleId(roleId);

        KeyDiffUtil.handle(systemRoleMenuIds, Arrays.asList(req.getMenuIds().split(",")),
                (removeKeys) ->
                        Assert.isTrue(
                                systemRoleMenuService.deleteByRoleIdAndMenuIds(roleId, removeKeys),
                                "移除该角色的菜单权限失败"),
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
        List<String> systemRolePermissionIds = systemRolePermissionService.listPermissionIdsByRoleId(roleId);

        KeyDiffUtil.handle(systemRolePermissionIds, Arrays.asList(req.getPermissionIds().split(",")),
                (removeKeys) ->
                        Assert.isTrue(
                                systemRolePermissionService.deleteByRoleIdAndPermissionIds(roleId, removeKeys),
                                "移除该角色的菜单权限失败"),
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
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 获取最新的权限信息
        return RetBuilder.success(buildRoleResp(systemRole, roleId));
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色")
    @PostMapping("remove/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:remove')")
    @StrixLog(operationGroup = "系统角色", operationName = "删除角色", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "角色 ID") @PathVariable String roleId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));
        Assert.isTrue(systemRole.getBuiltin() == CommonFlag.NO, I18nUtil.get("assert.role.builtinNoDelete"));

        systemRoleService.deleteRoleWithRelations(systemRole);

        return RetBuilder.success();
    }

    /**
     * 移除角色的菜单权限
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    @Operation(summary = "移除角色菜单关联")
    @PostMapping("remove/{roleId}/menu/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @StrixLog(operationGroup = "系统角色", operationName = "移除角色的菜单权限", operationType = SystemLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRoleMenu(@Parameter(description = "角色 ID") @PathVariable String roleId, @Parameter(description = "菜单 ID") @PathVariable String menuId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));
        Assert.isTrue(systemRole.getBuiltin() == CommonFlag.NO, I18nUtil.get("assert.role.builtinNoModify"));

        // 查询该菜单和其子菜单的id注意此处使用了ram缓存
        List<String> menuAndChildrenMenu = systemMenuCache.getIdListByParentMenu(menuId);

        systemRoleMenuService.deleteByRoleIdAndMenuIds(roleId, menuAndChildrenMenu);

        // 刷新redis缓存
        systemMenuCache.updateRedisBySystemRoleId(roleId);
        // 刷新 redis 中的登录用户信息
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 返回移除后的最新关系信息
        return RetBuilder.success(buildRoleResp(systemRole, roleId));
    }

    /**
     * 移除角色的系统权限
     *
     * @param roleId       角色id
     * @param permissionId 系统权限id
     */
    @Operation(summary = "移除角色权限关联")
    @PostMapping("remove/{roleId}/permission/{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:role:modifyPermission')")
    @StrixLog(operationGroup = "系统角色", operationName = "移除角色的系统权限", operationType = SystemLogOperType.UPDATE)
    public RetResult<SystemRoleResp> removeRolePermission(@Parameter(description = "角色 ID") @PathVariable String roleId, @Parameter(description = "权限 ID") @PathVariable String permissionId) {
        SystemRole systemRole = systemRoleService.getById(roleId);
        Assert.notNull(systemRole, I18nUtil.notFound("field.systemRole"));
        Assert.isTrue(systemRole.getBuiltin() == CommonFlag.NO, I18nUtil.get("assert.role.builtinNoModify"));

        systemRolePermissionService.deleteByRoleIdAndPermissionId(roleId, permissionId);

        // 刷新redis缓存
        systemPermissionCache.updateRedisBySystemRoleId(roleId);
        // 刷新 redis 中的登录用户信息
        systemManagerService.refreshLoginInfoByRole(roleId);

        // 返回移除后的最新关系信息
        return RetBuilder.success(buildRoleResp(systemRole, roleId));
    }

    /**
     * 获取系统角色下拉列表
     */
    @Operation(summary = "获取角色下拉列表")
    @GetMapping("select")
    public RetResult<CommonSelectDataResp> getSystemRoleSelectList() {
        return RetBuilder.success(systemRoleService.getSelectData());
    }

    /**
     * 获取系统角色穿梭框数据
     */
    @Operation(summary = "获取穿梭框数据")
    @GetMapping("transfer")
    public RetResult<CommonTransferDataResp> getTransferData() {
        List<SystemRole> systemRoleList = systemRoleService.listForTransfer();

        return RetBuilder.success(new CommonTransferDataResp(systemRoleList, "id", "name", null));
    }

    private SystemRoleResp buildRoleResp(SystemRole systemRole, String roleId) {
        List<SystemMenu> menusByRoleId = systemRoleService.getMenusByRoleId(systemRole.getId());
        List<SystemPermission> systemPermissionByRoleId = systemRoleService.getSystemPermissionByRoleId(roleId);
        List<SystemMenuListResp.SystemMenuManageItem> menuItems = new SystemMenuListResp(menusByRoleId, systemPermissionByRoleId).getSystemMenuList();
        List<SystemPermissionListResp.SystemPermissionItem> permissionList = new SystemPermissionListResp(systemPermissionByRoleId).getSystemPermissionList();
        return new SystemRoleResp(systemRole.getId(), systemRole.getName(), systemRole.getRegionPermissionType(), menuItems, permissionList);
    }

}
