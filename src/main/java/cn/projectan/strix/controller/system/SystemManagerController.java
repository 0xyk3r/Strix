package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.db.system.SystemManagerRole;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.dict.system.SystemManagerStatus;
import cn.projectan.strix.model.dict.system.SystemManagerType;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerListReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerUpdateReq;
import cn.projectan.strix.model.response.common.CommonTransferDataResp;
import cn.projectan.strix.model.response.system.manager.SystemManagerListResp;
import cn.projectan.strix.model.response.system.manager.SystemManagerResp;
import cn.projectan.strix.service.system.SystemManagerRoleService;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import cn.projectan.strix.util.crypto.StrixSM3Util;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 系统人员
 *
 * @author ProjectAn
 * @since 2021/6/11 17:40
 */
@Slf4j
@RestController
@RequestMapping("system/manager")
@RequiredArgsConstructor
@Tag(name = "系统 - 管理员管理")
public class SystemManagerController extends BaseSystemController {

    private final SystemManagerService systemManagerService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final TokenSessionService tokenSessionService;

    /**
     * 查询人员列表
     */
    @Operation(summary = "管理员列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员列表")
    public RetResult<SystemManagerListResp> getSystemManagerList(SystemManagerListReq req) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();

        // 如果指定了角色ID，先查询拥有该角色的所有管理员ID
        List<String> managerIdsByRole;
        if (StringUtils.hasText(req.getRoleId())) {
            managerIdsByRole = systemManagerRoleService.listManagerIdsByRoleId(req.getRoleId());

            // 如果没有任何管理员拥有该角色，直接返回空结果
            if (CollectionUtils.isEmpty(managerIdsByRole)) {
                return RetBuilder.success(new SystemManagerListResp(Collections.emptyList(), 0L));
            }
        }

        Page<SystemManager> page = systemManagerService.listPage(req, loginManagerRegionPermissions);

        SystemManagerListResp resp = new SystemManagerListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 查询人员信息
     */
    @Operation(summary = "管理员详情")
    @GetMapping("{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员信息")
    public RetResult<SystemManagerResp> getSystemManager(@Parameter(description = "管理员 ID") @PathVariable String managerId) {
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        List<String> systemManagerRoleIds = systemManagerService.getRoleIdListByManagerId(managerId);

        return RetBuilder.success(new SystemManagerResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getStatus(), systemManager.getType(), systemManager.getRegionId(), systemManager.getCreatedTime(), String.join(",", systemManagerRoleIds)));
    }

    /**
     * 更改人员信息
     */
    @Operation(summary = "修改管理员字段")
    @PostMapping("modify/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "更改人员信息", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> modifyField(@Parameter(description = "管理员 ID") @PathVariable String managerId, @RequestBody @Validated SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(systemManager.getBuiltin() == CommonFlag.NO, "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        LambdaUpdateWrapper<SystemManager> systemManagerUpdateWrapper = new LambdaUpdateWrapper<>();
        systemManagerUpdateWrapper.eq(SystemManager::getId, managerId);

        AtomicBoolean needReturnNewData = new AtomicBoolean(false);

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(SystemManagerStatus.valid(Short.parseShort(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set(SystemManager::getStatus, req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "type" -> {
                Assert.isTrue(SystemManagerType.valid(Short.parseShort(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set(SystemManager::getType, req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "role" -> {
                // 修改管理用户的角色
                List<String> systemManagerRoleIds = systemManagerService.getRoleIdListByManagerId(managerId);
                KeyDiffUtil.handle(systemManagerRoleIds, Arrays.asList(req.getValue().split(",")),
                        (removeKeys) ->
                                Assert.isTrue(
                                        systemManagerRoleService.deleteByManagerIdAndRoleIds(managerId, removeKeys),
                                        "移除该管理用户的角色失败"),
                        (addKeys) -> {
                            List<SystemManagerRole> systemManagerRoleList = addKeys.stream()
                                    .map(k -> new SystemManagerRole(managerId, k))
                                    .collect(Collectors.toList());
                            Assert.isTrue(systemManagerRoleService.saveBatch(systemManagerRoleList), "增加该角色的菜单权限失败");
                        },
                        () -> {
                            // 刷新redis缓存和会话
                            systemManagerService.refreshLoginInfoByManager(managerId);
                            needReturnNewData.set(true);
                        }
                );
            }
            default -> {
                return RetBuilder.error("参数错误");
            }
        }

        if (needReturnNewData.get()) {
            List<String> systemManagerRoleIds = systemManagerService.getRoleIdListByManagerId(managerId);
            return RetBuilder.success(new SystemManagerResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getStatus(), systemManager.getType(), systemManager.getRegionId(), systemManager.getCreatedTime(), String.join(",", systemManagerRoleIds)));
        }

        return RetBuilder.success();
    }

    /**
     * 新增人员
     */
    @Operation(summary = "新增管理员")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:manager:add')")
    @StrixLog(operationGroup = "系统人员", operationName = "新增人员", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemManagerUpdateReq req) {
        Assert.notNull(req, "参数错误");
        checkLoginManagerRegionPermission(req.getRegionId());

        SystemManager systemManager = new SystemManager(
                req.getNickname(),
                req.getLoginName(),
                StrixSM3Util.hashPassword(req.getLoginPassword()),
                req.getStatus(),
                req.getType(),
                req.getRegionId(),
                CommonFlag.NO
        );

        UniqueChecker.check(systemManager);

        Assert.isTrue(systemManagerService.save(systemManager), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改人员
     */
    @Operation(summary = "编辑管理员")
    @PostMapping("update/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "修改人员", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "管理员 ID") @PathVariable String managerId, @RequestBody @Validated(UpdateGroup.class) SystemManagerUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(systemManager.getBuiltin() == CommonFlag.NO, "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        // 若提交了新密码，先对明文进行 SM3 哈希
        if (StringUtils.hasText(req.getLoginPassword())) {
            req.setLoginPassword(StrixSM3Util.hashPassword(req.getLoginPassword()));
        }

        LambdaUpdateWrapper<SystemManager> updateWrapper = UpdateBuilder.build(systemManager, req);
        UniqueChecker.check(systemManager);
        Assert.isTrue(systemManagerService.update(updateWrapper), "保存失败");

        systemManagerService.refreshLoginInfoByManager(managerId);

        return RetBuilder.success();
    }

    /**
     * 删除人员
     */
    @Operation(summary = "删除管理员")
    @PostMapping("remove/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:remove')")
    @StrixLog(operationGroup = "系统人员", operationName = "删除人员", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "管理员 ID") @PathVariable String managerId) {
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(systemManager.getBuiltin() == CommonFlag.NO, "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        systemManagerService.removeById(systemManager);

        // 删除管理人员和角色间关系
        systemManagerRoleService.deleteByManagerId(systemManager.getId());

        // 使登录Token失效
        tokenSessionService.invalidateManagerSession(systemManager.getId());

        return RetBuilder.success();
    }

    /**
     * 获取系统人员穿梭框数据
     */
    @Operation(summary = "获取穿梭框数据")
    @GetMapping("transfer")
    public RetResult<CommonTransferDataResp> getTransferData() {
        List<SystemManager> systemManagerList = systemManagerService.listForTransfer();

        return RetBuilder.success(new CommonTransferDataResp(systemManagerList, "id", "nickname", null));
    }

}
