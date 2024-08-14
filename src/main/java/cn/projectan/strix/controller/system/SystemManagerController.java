package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.core.cache.SystemRegionCache;
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
import cn.projectan.strix.utils.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ProjectAn
 * @date 2021/6/11 17:40
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
    private final SystemRegionCache systemRegionCache;
    private final RedisUtil redisUtil;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员列表")
    public RetResult<SystemManagerListResp> getSystemManagerList(SystemManagerListReq req) {
        QueryWrapper<SystemManager> systemManagerQueryWrapper = new QueryWrapper<>();
        RegionPermissionTool.appendRegionPermissionToQueryWrapper("region_id", systemManagerQueryWrapper);
        if (StringUtils.hasText(req.getKeyword())) {
            systemManagerQueryWrapper.like("nickname", req.getKeyword())
                    .or(q -> q.like("login_name", req.getKeyword()));
        }
        if (NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE)) {
            systemManagerQueryWrapper.eq("status", req.getStatus());
        }
        if (NumUtil.checkCategory(req.getType(), NumCategory.POSITIVE)) {
            systemManagerQueryWrapper.eq("type", req.getType());
        }
        systemManagerQueryWrapper.orderByAsc("create_time");

        Page<SystemManager> page = systemManagerService.page(req.getPage(), systemManagerQueryWrapper);

        SystemManagerListResp resp = new SystemManagerListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    @GetMapping("{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager')")
    @StrixLog(operationGroup = "系统人员", operationName = "查询人员信息")
    public RetResult<SystemManagerResp> getSystemManager(@PathVariable String managerId) {
        Assert.notNull(managerId, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        RegionPermissionTool.check(systemManager.getRegionId());

        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
        List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

        return RetBuilder.success(new SystemManagerResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getStatus(), systemManager.getType(), systemManager.getRegionId(), systemManager.getCreateTime(), String.join(",", systemManagerRoleIds)));
    }

    @PostMapping("modify/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "更改人员信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String managerId, @RequestBody SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        RegionPermissionTool.check(systemManager.getRegionId());

        UpdateWrapper<SystemManager> systemManagerUpdateWrapper = new UpdateWrapper<>();
        systemManagerUpdateWrapper.eq("id", managerId);

        AtomicBoolean needReturnNewData = new AtomicBoolean(false);

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(SystemManagerStatus.valid(Integer.valueOf(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set("status", req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "type" -> {
                Assert.isTrue(SystemManagerType.valid(Integer.parseInt(req.getValue())), "参数错误");
                systemManagerUpdateWrapper.set("type", req.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "role" -> {
                // 修改管理用户的角色
                QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
                systemManagerRoleQueryWrapper.select("system_role_id");
                systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
                List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
                KeyDiffUtil.handle(systemManagerRoleIds, Arrays.asList(req.getValue().split(",")),
                        (removeKeys) -> {
                            QueryWrapper<SystemManagerRole> removeQueryWrapper = new QueryWrapper<>();
                            removeQueryWrapper.eq("system_manager_id", managerId);
                            removeQueryWrapper.in("system_role_id", removeKeys);
                            Assert.isTrue(systemManagerRoleService.remove(removeQueryWrapper), "移除该管理用户的角色失败");
                        },
                        (addKeys) -> {
                            List<SystemManagerRole> systemManagerRoleList = new ArrayList<>();
                            addKeys.forEach(k -> {
                                SystemManagerRole systemManagerRole = new SystemManagerRole();
                                systemManagerRole.setSystemManagerId(managerId);
                                systemManagerRole.setSystemRoleId(k);
                                systemManagerRole.setCreateBy(loginManagerId());
                                systemManagerRole.setUpdateBy(loginManagerId());
                                systemManagerRoleList.add(systemManagerRole);
                            });
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
            QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
            systemManagerRoleQueryWrapper.select("system_role_id");
            systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
            List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

            return RetBuilder.success(new SystemManagerResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getStatus(), systemManager.getType(), systemManager.getRegionId(), systemManager.getCreateTime(), String.join(",", systemManagerRoleIds)));
        }

        return RetBuilder.success();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:manager:add')")
    @StrixLog(operationGroup = "系统人员", operationName = "新增人员", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemManagerUpdateReq req) {
        Assert.notNull(req, "参数错误");
        RegionPermissionTool.check(req.getRegionId());

        SystemManager systemManager = new SystemManager(
                req.getNickname(),
                req.getLoginName(),
                req.getLoginPassword(),
                req.getStatus(),
                req.getType(),
                req.getRegionId(),
                BuiltinConstant.NO
        );

        UniqueDetectionTool.check(systemManager);

        Assert.isTrue(systemManagerService.save(systemManager), "保存失败");

        return RetBuilder.success();
    }

    @PostMapping("update/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:update')")
    @StrixLog(operationGroup = "系统人员", operationName = "修改人员", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String managerId, @RequestBody @Validated(UpdateGroup.class) SystemManagerUpdateReq req) {
        Assert.hasText(managerId, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        RegionPermissionTool.check(systemManager.getRegionId());

        UpdateWrapper<SystemManager> updateWrapper = UpdateConditionBuilder.build(systemManager, req);
        UniqueDetectionTool.check(systemManager);
        Assert.isTrue(systemManagerService.update(updateWrapper), "保存失败");

        systemManagerService.refreshLoginInfoByManager(managerId);

        return RetBuilder.success();
    }

    @PostMapping("remove/{managerId}")
    @PreAuthorize("@ss.hasPermission('system:manager:remove')")
    @StrixLog(operationGroup = "系统人员", operationName = "删除人员", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String managerId) {
        Assert.hasText(managerId, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        Assert.isTrue(BuiltinConstant.NO == systemManager.getBuiltin(), "内置用户不允许修改");
        RegionPermissionTool.check(systemManager.getRegionId());

        systemManagerService.removeById(systemManager);

        // 删除管理人员和角色间关系
        QueryWrapper<SystemManagerRole> deleteManagerRoleRelationQueryWrapper = new QueryWrapper<>();
        deleteManagerRoleRelationQueryWrapper.eq("system_manager_id", systemManager.getId());
        systemManagerRoleService.remove(deleteManagerRoleRelationQueryWrapper);

        // 使登录Token失效
        Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        if (existToken != null) {
            redisUtil.del("strix:system:manager:login_token:token:" + existToken);
            redisUtil.del("strix:system:manager:login_token:login:id_" + systemManager.getId());
        }

        return RetBuilder.success();
    }

}
