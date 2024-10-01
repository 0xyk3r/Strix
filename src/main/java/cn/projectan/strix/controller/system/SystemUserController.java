package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.SystemUserRelation;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.dict.SystemUserStatus;
import cn.projectan.strix.model.enums.NumCategory;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.user.SystemUserListReq;
import cn.projectan.strix.model.request.system.user.SystemUserUpdateReq;
import cn.projectan.strix.model.response.system.user.SystemUserListResp;
import cn.projectan.strix.model.response.system.user.SystemUserResp;
import cn.projectan.strix.service.SystemUserRelationService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.NumUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户
 *
 * @author ProjectAn
 * @date 2021/8/27 14:20
 */
@Slf4j
@RestController
@RequestMapping("system/user")
@RequiredArgsConstructor
public class SystemUserController extends BaseSystemController {

    private final SystemUserService systemUserService;
    private final SystemUserRelationService systemUserRelationService;

    /**
     * 查询用户列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @StrixLog(operationGroup = "系统用户", operationName = "查询用户列表")
    public RetResult<SystemUserListResp> getSystemUserList(SystemUserListReq req) {
        Page<SystemUser> page = systemUserService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SystemUser::getNickname, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SystemUser::getPhoneNumber, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), SystemUser::getStatus, req.getStatus())
                .orderByAsc(SystemUser::getCreateTime)
                .page(req.getPage());

        SystemUserListResp resp = new SystemUserListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 查询用户信息
     */
    @GetMapping("{userId}")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @StrixLog(operationGroup = "系统用户", operationName = "查询用户信息")
    public RetResult<SystemUserResp> getSystemUser(@PathVariable String userId) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        return RetBuilder.success(new SystemUserResp(systemUser));
    }

    /**
     * 修改用户信息
     */
    @PostMapping("modify/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "更改用户信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String userId, @RequestBody SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        LambdaUpdateWrapper<SystemUser> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(SystemUser::getId, userId);

        switch (req.getField()) {
            case "nickname" -> queryWrapper.set(SystemUser::getNickname, req.getValue());
            case "status" -> {
                Assert.isTrue(SystemUserStatus.valid(Integer.parseInt(req.getValue())), "参数错误");
                queryWrapper.set(SystemUser::getStatus, req.getValue());
            }
            case "phoneNumber" -> queryWrapper.set(SystemUser::getPhoneNumber, req.getValue());
            default -> {
                return RetBuilder.error("参数错误");
            }
        }

        Assert.isTrue(systemUserService.update(queryWrapper), "修改失败");

        return RetBuilder.success();
    }

    /**
     * 新增用户
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:user:add')")
    @StrixLog(operationGroup = "系统用户", operationName = "新增用户", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemUserUpdateReq req) {
        Assert.notNull(req, "参数错误");

        SystemUser systemUser = new SystemUser(
                req.getNickname(),
                req.getStatus(),
                req.getPhoneNumber(),
                null,
                null
        );

        UniqueDetectionTool.check(systemUser);

        Assert.isTrue(systemUserService.save(systemUser), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改用户
     */
    @PostMapping("update/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "修改用户", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String userId, @RequestBody @Validated(UpdateGroup.class) SystemUserUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        LambdaUpdateWrapper<SystemUser> updateWrapper = UpdateConditionBuilder.build(systemUser, req);
        UniqueDetectionTool.check(systemUser);
        Assert.isTrue(systemUserService.update(updateWrapper), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 删除用户
     */
    @PostMapping("remove/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:remove')")
    @StrixLog(operationGroup = "系统用户", operationName = "删除用户", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String userId) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统角色信息不存在");

        systemUserService.removeById(systemUser);

        // 删除角色的第三方账号绑定关系
        systemUserRelationService.lambdaUpdate()
                .eq(SystemUserRelation::getSystemUserId, systemUser.getId())
                .remove();

        return RetBuilder.success();
    }

}
