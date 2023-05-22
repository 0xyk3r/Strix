package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ramcache.SystemMenuCache;
import cn.projectan.strix.core.ramcache.SystemPermissionCache;
import cn.projectan.strix.core.ramcache.SystemRegionCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.constant.SystemManagerStatus;
import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemManagerRole;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerListQueryReq;
import cn.projectan.strix.model.request.system.manager.SystemManagerUpdateReq;
import cn.projectan.strix.model.response.system.manager.SystemManagerListQueryResp;
import cn.projectan.strix.model.response.system.manager.SystemManagerQueryByIdResp;
import cn.projectan.strix.service.SystemManagerRoleService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.utils.KeysDiffHandler;
import cn.projectan.strix.utils.NumUtils;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author 安炯奕
 * @date 2021/6/11 17:40
 */
@Slf4j
@RestController
@RequestMapping("system/manager")
public class SystemManagerController extends BaseSystemController {

    @Autowired
    private SystemManagerService systemManagerService;
    @Autowired
    private SystemManagerRoleService systemManagerRoleService;

    @Autowired
    private SystemMenuCache systemMenuCache;
    @Autowired
    private SystemPermissionCache systemPermissionCache;
    @Autowired
    private SystemRegionCache systemRegionCache;

    @GetMapping("")
    @PreAuthorize("@ss.hasRead('System_Manager')")
    public RetResult<SystemManagerListQueryResp> getSystemManagerList(SystemManagerListQueryReq systemManagerListQueryReq) {
        QueryWrapper<SystemManager> systemManagerQueryWrapper = new QueryWrapper<>();
        if (!isSuperManager()) {
            // 非超级管理员用户 根据地区权限查询
            systemManagerQueryWrapper.eq("manager_type", SystemManagerType.PLATFORM_ACCOUNT);
            systemManagerQueryWrapper.in("region_id", getLoginManagerRegionIdList());
        }
        if (StringUtils.hasText(systemManagerListQueryReq.getKeyword())) {
            systemManagerQueryWrapper.like("nickname", systemManagerListQueryReq.getKeyword())
                    .or(q -> q.like("login_name", systemManagerListQueryReq.getKeyword()));
        }
        if (NumUtils.isNonnegativeNumber(systemManagerListQueryReq.getManagerStatus())) {
            systemManagerQueryWrapper.eq("manager_status", systemManagerListQueryReq.getManagerStatus());
        }
        if (NumUtils.isPositiveNumber(systemManagerListQueryReq.getManagerType())) {
            systemManagerQueryWrapper.eq("manager_type", systemManagerListQueryReq.getManagerType());
        }
        systemManagerQueryWrapper.orderByAsc("create_time");

        Page<SystemManager> page = systemManagerService.page(systemManagerListQueryReq.getPage(), systemManagerQueryWrapper);

        SystemManagerListQueryResp resp = new SystemManagerListQueryResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }

    @GetMapping("{managerId}")
    @PreAuthorize("@ss.hasRead('System_Manager')")
    public RetResult<SystemManagerQueryByIdResp> getSystemManager(@PathVariable String managerId) {
        Assert.notNull(managerId, "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");

        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_manager_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
        List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

        return RetMarker.makeSuccessRsp(new SystemManagerQueryByIdResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getManagerStatus(), systemManager.getManagerType(), systemManager.getRegionId(), systemManager.getCreateTime(), String.join(",", systemManagerRoleIds)));
    }

    @PostMapping("modify/{managerId}")
    @PreAuthorize("@ss.hasWrite('System_Manager')")
    public RetResult<Object> modifyField(@PathVariable String managerId, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");
        if (!"region".equals(singleFieldModifyReq.getField())) {
            Assert.isTrue(!"anjiongyi".equals(managerId), "该用户不允许编辑或删除");
        }
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");

        UpdateWrapper<SystemManager> systemManagerUpdateWrapper = new UpdateWrapper<>();
        systemManagerUpdateWrapper.eq("id", managerId);

        AtomicBoolean needReturnNewData = new AtomicBoolean(false);

        switch (singleFieldModifyReq.getField()) {
            case "managerStatus" -> {
                Assert.isTrue(SystemManagerStatus.valid(Integer.valueOf(singleFieldModifyReq.getValue())), "参数错误");
                systemManagerUpdateWrapper.set("manager_status", singleFieldModifyReq.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "managerType" -> {
                Assert.isTrue(SystemManagerType.valid(Integer.parseInt(singleFieldModifyReq.getValue())), "参数错误");
                systemManagerUpdateWrapper.set("manager_type", singleFieldModifyReq.getValue());
                Assert.isTrue(systemManagerService.update(systemManagerUpdateWrapper), "修改失败");
            }
            case "role" -> {
                // 修改管理用户的角色
                QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
                systemManagerRoleQueryWrapper.select("system_manager_role_id");
                systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
                List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
                KeysDiffHandler.handle(systemManagerRoleIds, Arrays.asList(singleFieldModifyReq.getValue().split(",")), (removeKeys, addKeys) -> {
                    if (removeKeys.size() > 0) {
                        QueryWrapper<SystemManagerRole> removeQueryWrapper = new QueryWrapper<>();
                        removeQueryWrapper.eq("system_manager_id", managerId);
                        removeQueryWrapper.in("system_manager_role_id", removeKeys);
                        Assert.isTrue(systemManagerRoleService.remove(removeQueryWrapper), "移除该管理用户的角色失败");
                    }
                    if (addKeys.size() > 0) {
                        List<SystemManagerRole> systemManagerRoleList = new ArrayList<>();
                        addKeys.forEach(k -> {
                            SystemManagerRole systemManagerRole = new SystemManagerRole();
                            systemManagerRole.setSystemManagerId(managerId);
                            systemManagerRole.setSystemManagerRoleId(k);
                            systemManagerRole.setCreateBy(getLoginManagerId());
                            systemManagerRole.setUpdateBy(getLoginManagerId());
                            systemManagerRoleList.add(systemManagerRole);
                        });
                        Assert.isTrue(systemManagerRoleService.saveBatch(systemManagerRoleList), "增加该角色的菜单权限失败");
                    }
                    // 刷新redis缓存
                    systemMenuCache.updateRedisBySystemManageId(managerId);
                    systemPermissionCache.updateRedisBySystemManageId(managerId);
                    needReturnNewData.set(true);
                });
            }
            default -> {
                return RetMarker.makeErrRsp("参数错误");
            }
        }

        if (needReturnNewData.get()) {
            QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
            systemManagerRoleQueryWrapper.select("system_manager_role_id");
            systemManagerRoleQueryWrapper.eq("system_manager_id", managerId);
            List<String> systemManagerRoleIds = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);

            return RetMarker.makeSuccessRsp(new SystemManagerQueryByIdResp(systemManager.getId(), systemManager.getNickname(), systemManager.getLoginName(), systemManager.getManagerStatus(), systemManager.getManagerType(), systemManager.getRegionId(), systemManager.getCreateTime(), String.join(",", systemManagerRoleIds)));
        }

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasWrite('System_Manager')")
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemManagerUpdateReq systemManagerUpdateReq) {
        Assert.notNull(systemManagerUpdateReq, "参数错误");
        Assert.isTrue(SystemManagerStatus.valid(systemManagerUpdateReq.getManagerStatus()), "参数错误");
        Assert.isTrue(SystemManagerType.valid(systemManagerUpdateReq.getManagerType()), "参数错误");

        SystemManager systemManager = new SystemManager(
                systemManagerUpdateReq.getNickname(),
                systemManagerUpdateReq.getLoginName(),
                systemManagerUpdateReq.getLoginPassword(),
                systemManagerUpdateReq.getManagerStatus(),
                systemManagerUpdateReq.getManagerType(),
                systemManagerUpdateReq.getRegionId()
        );
        systemManager.setCreateBy(getLoginManagerId());
        systemManager.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(systemManager);

        Assert.isTrue(systemManagerService.save(systemManager), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{managerId}")
    @PreAuthorize("@ss.hasWrite('System_Manager')")
    public RetResult<Object> update(@PathVariable String managerId, @RequestBody @Validated(ValidationGroup.Update.class) SystemManagerUpdateReq systemManagerUpdateReq) {
        Assert.hasText(managerId, "参数错误");
        Assert.isTrue(!"anjiongyi".equals(managerId), "该用户不允许编辑或删除");
        Assert.notNull(systemManagerUpdateReq, "参数错误");
        Assert.isTrue(SystemManagerStatus.valid(systemManagerUpdateReq.getManagerStatus()), "参数错误");
        Assert.isTrue(SystemManagerType.valid(systemManagerUpdateReq.getManagerType()), "参数错误");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");

        UpdateWrapper<SystemManager> updateWrapper = UpdateConditionBuilder.build(systemManager, systemManagerUpdateReq, getLoginManagerId());
        UniqueDetectionTool.check(systemManager);
        Assert.isTrue(systemManagerService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{managerId}")
    @PreAuthorize("@ss.hasWrite('System_Manager')")
    public RetResult<Object> remove(@PathVariable String managerId) {
        Assert.hasText(managerId, "参数错误");
        Assert.isTrue(!"anjiongyi".equals(managerId), "该用户不允许编辑或删除");
        SystemManager systemManager = systemManagerService.getById(managerId);
        Assert.notNull(systemManager, "系统人员信息不存在");
        // TODO 权限验证
        systemManagerService.removeById(systemManager);

        // 删除管理人员和角色间关系
        QueryWrapper<SystemManagerRole> deleteManagerRoleRelationQueryWrapper = new QueryWrapper<>();
        deleteManagerRoleRelationQueryWrapper.eq("system_manager_id", systemManager.getId());
        systemManagerRoleService.remove(deleteManagerRoleRelationQueryWrapper);

        return RetMarker.makeSuccessRsp();
    }

}
