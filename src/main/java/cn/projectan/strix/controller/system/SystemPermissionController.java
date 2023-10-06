package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRolePermission;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.system.permission.SystemPermissionUpdateReq;
import cn.projectan.strix.model.response.common.CommonTransferDataResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionResp;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.service.SystemRolePermissionService;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/6 16:20
 */
@Slf4j
@RestController
@RequestMapping("system/permission")
@RequiredArgsConstructor
public class SystemPermissionController extends BaseSystemController {

    private final SystemPermissionService systemPermissionService;
    private final SystemRolePermissionService systemRolePermissionService;
    private final SystemPermissionCache systemPermissionCache;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统权限", operationName = "查询权限列表")
    public RetResult<SystemPermissionListResp> getSystemPermissionList() {
        QueryWrapper<SystemPermission> systemPermissionQueryWrapper = new QueryWrapper<>();
        systemPermissionQueryWrapper.orderByAsc("create_time");
        List<SystemPermission> systemPermissionList = systemPermissionService.list(systemPermissionQueryWrapper);

        return RetMarker.makeSuccessRsp(new SystemPermissionListResp(systemPermissionList));
    }

    @GetMapping("{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统权限", operationName = "查询权限信息")
    public RetResult<SystemPermissionResp> getSystemPermission(@PathVariable String permissionId) {
        Assert.notNull(permissionId, "参数错误");
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, "系统权限信息不存在");

        return RetMarker.makeSuccessRsp(new SystemPermissionResp(systemPermission.getId(), systemPermission.getName(), systemPermission.getKey(), systemPermission.getMenuId(), systemPermission.getDescription()));
    }

    @PostMapping("update")
    @PreAuthorize("@ss.anyPermission('system:menu:add', 'system:menu:update')")
    @StrixLog(operationGroup = "系统权限", operationName = "新增权限", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemPermissionUpdateReq req) {
        Assert.notNull(req, "参数错误");

        SystemPermission systemPermission = new SystemPermission(
                req.getName(),
                req.getKey(),
                req.getMenuId(),
                req.getDescription()
        );
        systemPermission.setCreateBy(loginManagerId());
        systemPermission.setUpdateBy(loginManagerId());

        UniqueDetectionTool.check(systemPermission);
        Assert.isTrue(systemPermissionService.save(systemPermission), "保存失败");
        systemPermissionCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{permissionId}")
    @PreAuthorize("@ss.anyPermission('system:menu:add', 'system:menu:update')")
    @StrixLog(operationGroup = "系统权限", operationName = "修改权限", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String permissionId, @RequestBody @Validated(UpdateGroup.class) SystemPermissionUpdateReq req) {
        Assert.hasText(permissionId, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, "系统权限信息不存在");

        UpdateWrapper<SystemPermission> updateWrapper = UpdateConditionBuilder.build(systemPermission, req);
        UniqueDetectionTool.check(systemPermission);
        Assert.isTrue(systemPermissionService.update(updateWrapper), "保存失败");
        // 更新缓存
        systemPermissionCache.updateRamAndRedis();
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByPermission(permissionId);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{permissionId}")
    @PreAuthorize("@ss.hasPermission('system:menu:remove')")
    @StrixLog(operationGroup = "系统权限", operationName = "删除权限", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String permissionId) {
        Assert.hasText(permissionId, "参数错误");
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, "系统权限信息不存在");

        systemPermissionService.removeById(systemPermission);
        // 删除角色和系统权限间关系
        QueryWrapper<SystemRolePermission> deleteRolePermissionRelationQueryWrapper = new QueryWrapper<>();
        deleteRolePermissionRelationQueryWrapper.eq("system_permission_id", systemPermission.getId());
        systemRolePermissionService.remove(deleteRolePermissionRelationQueryWrapper);
        systemPermissionCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("transfer")
    public RetResult<CommonTransferDataResp> getTransferData() {
        QueryWrapper<SystemPermission> systemPermissionQueryWrapper = new QueryWrapper<>();
        systemPermissionQueryWrapper.select("id", "name");
        List<SystemPermission> systemPermissionList = systemPermissionService.list(systemPermissionQueryWrapper);

        return RetMarker.makeSuccessRsp(new CommonTransferDataResp(systemPermissionList, "id", "name", null));
    }

}
