package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ramcache.SystemPermissionCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.constant.SystemPermissionType;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRolePermission;
import cn.projectan.strix.model.request.system.systempermission.SystemPermissionUpdateReq;
import cn.projectan.strix.model.response.common.CommonTransferDataResp;
import cn.projectan.strix.model.response.system.systempermission.SystemPermissionListQueryResp;
import cn.projectan.strix.model.response.system.systempermission.SystemPermissionQueryByIdResp;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.service.SystemRolePermissionService;
import cn.projectan.strix.utils.StrixAssert;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SystemPermissionController extends BaseSystemController {

    @Autowired
    private SystemPermissionService systemPermissionService;
    @Autowired
    private SystemRolePermissionService systemRolePermissionService;

    @Autowired
    private SystemPermissionCache systemPermissionCache;

    @GetMapping("")
    @PreAuthorize("@ss.hasRead('System_Permission')")
    public RetResult<SystemPermissionListQueryResp> getSystemPermissionList() {
        QueryWrapper<SystemPermission> systemPermissionQueryWrapper = new QueryWrapper<>();
        systemPermissionQueryWrapper.orderByAsc("create_time");
        List<SystemPermission> systemPermissionList = systemPermissionService.list(systemPermissionQueryWrapper);

        return RetMarker.makeSuccessRsp(new SystemPermissionListQueryResp(systemPermissionList));
    }

    @GetMapping("{permissionId}")
    @PreAuthorize("@ss.hasRead('System_Permission')")
    public RetResult<SystemPermissionQueryByIdResp> getSystemPermission(@PathVariable String permissionId) {
        Assert.notNull(permissionId, "参数错误");
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, "系统权限信息不存在");

        return RetMarker.makeSuccessRsp(new SystemPermissionQueryByIdResp(systemPermission.getId(), systemPermission.getName(), systemPermission.getPermissionKey(), systemPermission.getPermissionType(), systemPermission.getDescription()));
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasWrite('System_Permission')")
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemPermissionUpdateReq systemPermissionUpdateReq) {
        Assert.notNull(systemPermissionUpdateReq, "参数错误");
        StrixAssert.in(systemPermissionUpdateReq.getPermissionType(), "参数错误", SystemPermissionType.READ_ONLY, SystemPermissionType.READ_WRITE);

        SystemPermission systemPermission = new SystemPermission(
                systemPermissionUpdateReq.getName(),
                systemPermissionUpdateReq.getPermissionKey(),
                systemPermissionUpdateReq.getPermissionType(),
                systemPermissionUpdateReq.getDescription()
        );
        systemPermission.setCreateBy(getLoginManagerId());
        systemPermission.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(systemPermission);
        Assert.isTrue(systemPermissionService.save(systemPermission), "保存失败");
        systemPermissionCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{permissionId}")
    @PreAuthorize("@ss.hasWrite('System_Permission')")
    public RetResult<Object> update(@PathVariable String permissionId, @RequestBody @Validated(ValidationGroup.Update.class) SystemPermissionUpdateReq systemPermissionUpdateReq) {
        Assert.hasText(permissionId, "参数错误");
        Assert.notNull(systemPermissionUpdateReq, "参数错误");
        SystemPermission systemPermission = systemPermissionService.getById(permissionId);
        Assert.notNull(systemPermission, "系统权限信息不存在");

        UpdateWrapper<SystemPermission> updateWrapper = UpdateConditionBuilder.build(systemPermission, systemPermissionUpdateReq, getLoginManagerId());
        UniqueDetectionTool.check(systemPermission);
        Assert.isTrue(systemPermissionService.update(updateWrapper), "保存失败");
        systemPermissionCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{permissionId}")
    @PreAuthorize("@ss.hasWrite('System_Permission')")
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
        systemPermissionQueryWrapper.select("id", "name", "permission_type");
        List<SystemPermission> systemPermissionList = systemPermissionService.list(systemPermissionQueryWrapper);

        return RetMarker.makeSuccessRsp(new CommonTransferDataResp(systemPermissionList, "id", "name", "permissionType"));
    }

}
