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
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemManagerRole;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.dict.SystemManagerStatus;
import cn.projectan.strix.model.dict.SystemManagerType;
import cn.projectan.strix.model.enums.NumCategory;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerListReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerUpdateReq;
import cn.projectan.strix.model.response.system.manager.SystemManagerListResp;
import cn.projectan.strix.model.response.system.manager.SystemManagerResp;
import cn.projectan.strix.service.SystemManagerRoleService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.util.RedisUtil;
import cn.projectan.strix.util.UniqueChecker;
import cn.projectan.strix.util.UpdateBuilder;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
public class SystemManagerController extends BaseSystemController {

    private final SystemManagerService systemManagerService;
    private final SystemManagerRoleService systemManagerRoleService;
    private final SystemMenuCache systemMenuCache;
    private final SystemPermissionCache systemPermissionCache;
    private final RedisUtil redisUtil;

    /**
     * 查询人员列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员列表")
    public RetResult<SystemManagerListResp> getSystemManagerList(SystemManagerListReq req) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        Page<SystemManager> page = systemManagerService.lambdaQuery()
                .eq(StringUtils.hasText(req.getKeyword()), SystemManager::getNickname, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SystemManager::getLoginName, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), SystemManager::getStatus, req.getStatus())
                .eq(NumUtil.checkCategory(req.getType(), NumCategory.POSITIVE), SystemManager::getType, req.getType())
                .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemManager::getRegionId, loginManagerRegionPermissions)
                .orderByAsc(SystemManager::getCreatedTime)
                .page(req.getPage());

        SystemManagerListResp resp = new SystemManagerListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 查询人员信息
     */
    @GetMapping("{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员信息")
    public RetResult<SystemManagerResp> getSystemManager(@PathVariable String managerId) {
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        List<String> systemManagerRoleIds = systemManagerService.getRoleIdListByManagerId(managerId);

        return RetBuilder.success(new SystemManagerResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getStatus(), systemManager.getType(), systemManager.getRegionId(), systemManager.getCreatedTime(), String.join(",", systemManagerRoleIds)));
    }

    /**
     * 更改人员信息
     */
    @PostMapping("modify/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "更改人员信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String managerId, @RequestBody SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        LambdaUpdateWrapper<SystemManager> systemManagerUpdateWrapper = new LambdaUpdateWrapper<>();
        systemManagerUpdateWrapper.eq(SystemManager::getId, managerId);

        AtomicBoolean needReturnNewData = new AtomicBoolean(false);

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(SystemManagerStatus.valid(Integer.valueOf(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set(SystemManager::getStatus, req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "type" -> {
                Assert.isTrue(SystemManagerType.valid(Integer.parseInt(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set(SystemManager::getType, req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "role" -> {
                // 修改管理用户的角色
                List<String> systemManagerRoleIds = systemManagerService.getRoleIdListByManagerId(managerId);
                KeyDiffUtil.handle(systemManagerRoleIds, Arrays.asList(req.getValue().split(",")),
                        (removeKeys) -> {
                            Assert.isTrue(
                                    systemManagerRoleService.lambdaUpdate()
                                            .eq(SystemManagerRole::getSystemManagerId, managerId)
                                            .in(SystemManagerRole::getSystemRoleId, removeKeys)
                                            .remove(),
                                    "移除该管理用户的角色失败");
                        },
                        (addKeys) -> {
                            List<SystemManagerRole> systemManagerRoleList = addKeys.stream()
                                    .map(k -> new SystemManagerRole(managerId, k))
                                    .collect(Collectors.toList());
                            Assert.isTrue(systemManagerRoleService.saveBatch(systemManagerRoleList), "增加该角色的菜单权限失败");
                        },
                        () -> {
                            // 刷新redis缓存
                            systemMenuCache.updateRedisBySystemManageId(managerId);
                            systemPermissionCache.updateRedisBySystemManageId(managerId);
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
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:manager:add')")
    @StrixLog(operationGroup = "系统人员", operationName = "新增人员", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemManagerUpdateReq req) {
        Assert.notNull(req, "参数错误");
        checkLoginManagerRegionPermission(req.getRegionId());

        SystemManager systemManager = new SystemManager(
                req.getNickname(),
                req.getLoginName(),
                req.getLoginPassword(),
                req.getStatus(),
                req.getType(),
                req.getRegionId(),
                BuiltinConstant.NO
        );

        UniqueChecker.check(systemManager);

        Assert.isTrue(systemManagerService.save(systemManager), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改人员
     */
    @PostMapping("update/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "修改人员", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String managerId, @RequestBody @Validated(UpdateGroup.class) SystemManagerUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        LambdaUpdateWrapper<SystemManager> updateWrapper = UpdateBuilder.build(systemManager, req);
        UniqueChecker.check(systemManager);
        Assert.isTrue(systemManagerService.update(updateWrapper), "保存失败");

        systemManagerService.refreshLoginInfoByManager(managerId);

        return RetBuilder.success();
    }

    /**
     * 删除人员
     */
    @PostMapping("remove/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:remove')")
    @StrixLog(operationGroup = "系统人员", operationName = "删除人员", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String managerId) {
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        checkLoginManagerRegionPermission(systemManager.getRegionId());

        systemManagerService.removeById(systemManager);

        // 删除管理人员和角色间关系
        systemManagerRoleService.lambdaUpdate()
                .eq(SystemManagerRole::getSystemManagerId, systemManager.getId())
                .remove();

        // 使登录Token失效
        Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        if (existToken != null) {
            redisUtil.del("strix:system:manager:login_token:token:" + existToken);
            redisUtil.del("strix:system:manager:login_token:login:id_" + systemManager.getId());
        }

        return RetBuilder.success();
    }

}
