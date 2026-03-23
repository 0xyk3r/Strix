package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemPermission;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.permission.SystemPermissionUpdateReq;
import cn.projectan.strix.model.response.common.CommonTransferDataResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionResp;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemPermissionService;
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

import java.util.List;

/**
 * 系统权限
 *
 * @author ProjectAn
 * @since 2021/7/6 16:20
 */
@Slf4j
@RestController
@RequestMapping("system/permission")
@RequiredArgsConstructor
@Tag(name = "系统 - 权限管理")
public class SystemPermissionController extends BaseSystemController {

    private final SystemPermissionService systemPermissionService;
    private final SystemManagerService systemManagerService;
    private final SystemPermissionCache systemPermissionCache;

    /**
     * 查询权限列表
     */
    @Operation(summary = "权限列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统权限", operationName = "查询权限列表")
    public RetResult<SystemPermissionListResp> getSystemPermissionList() {
        List<SystemPermission> systemPermissionList = systemPermissionService.listAll();

        return RetBuilder.success(new SystemPermissionListResp(systemPermissionList));
    }

    /**
     * 查询权限信息
     */
    @Operation(summary = "权限详情")
    @GetMapping("{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统权限", operationName = "查询权限信息")
    public RetResult<SystemPermissionResp> getSystemPermission(@Parameter(description = "权限 ID") @PathVariable String permissionId) {
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, I18nUtil.notFound("field.systemPermission"));

        return RetBuilder.success(new SystemPermissionResp(systemPermission.getId(), systemPermission.getName(), systemPermission.getKey(), systemPermission.getMenuId(), systemPermission.getDescription()));
    }

    /**
     * 新增权限
     */
    @Operation(summary = "新增权限")
    @PostMapping("update")
    @PreAuthorize("@ss.anyPermission('system:menu:add', 'system:menu:update')")
    @StrixLog(operationGroup = "系统权限", operationName = "新增权限", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemPermissionUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));

        SystemPermission systemPermission = new SystemPermission(
                req.getName(),
                req.getKey(),
                req.getMenuId(),
                req.getDescription()
        );

        UniqueChecker.check(systemPermission);
        Assert.isTrue(systemPermissionService.save(systemPermission), "保存失败");
        systemPermissionCache.updateRamAndRedis();

        return RetBuilder.success();
    }

    /**
     * 修改权限
     */
    @Operation(summary = "编辑权限")
    @PostMapping("update/{permissionId}")
    @PreAuthorize("@ss.anyPermission('system:menu:add', 'system:menu:update')")
    @StrixLog(operationGroup = "系统权限", operationName = "修改权限", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "权限 ID") @PathVariable String permissionId, @RequestBody @Validated(UpdateGroup.class) SystemPermissionUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, I18nUtil.notFound("field.systemPermission"));

        LambdaUpdateWrapper<SystemPermission> updateWrapper = UpdateBuilder.build(systemPermission, req);
        UniqueChecker.check(systemPermission);
        Assert.isTrue(systemPermissionService.update(updateWrapper), "保存失败");
        // 更新缓存
        systemPermissionCache.updateRamAndRedis();
        // 刷新 redis 中的登录用户信息
        systemManagerService.refreshLoginInfoByPermission(permissionId);

        return RetBuilder.success();
    }

    /**
     * 删除权限
     */
    @Operation(summary = "删除权限")
    @PostMapping("remove/{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:menu:remove')")
    @StrixLog(operationGroup = "系统权限", operationName = "删除权限", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "权限 ID") @PathVariable String permissionId) {
        systemPermissionService.deleteByIds(List.of(permissionId));
        return RetBuilder.success();
    }

    /**
     * 权限穿梭框数据
     */
    @Operation(summary = "获取穿梭框数据")
    @GetMapping("transfer")
    public RetResult<CommonTransferDataResp> getTransferData() {
        List<SystemPermission> systemPermissionList = systemPermissionService.listForTransfer();

        return RetBuilder.success(new CommonTransferDataResp(systemPermissionList, "id", "name", null));
    }

}
