package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.constant.SysLogOperType;
import cn.projectan.strix.model.constant.SystemUserStatus;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.SystemUserRelation;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.user.SystemUserListReq;
import cn.projectan.strix.model.request.system.user.SystemUserUpdateReq;
import cn.projectan.strix.model.response.system.user.SystemUserListResp;
import cn.projectan.strix.model.response.system.user.SystemUserResp;
import cn.projectan.strix.service.SystemUserRelationService;
import cn.projectan.strix.service.SystemUserService;
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

/**
 * @author 安炯奕
 * @date 2021/8/27 14:20
 */
@Slf4j
@RestController
@RequestMapping("system/user")
public class SystemUserController extends BaseSystemController {

    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private SystemUserRelationService systemUserRelationService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @SysLog(operationGroup = "系统用户", operationName = "查询用户列表")
    public RetResult<SystemUserListResp> getSystemUserList(SystemUserListReq req) {
        QueryWrapper<SystemUser> systemUserQueryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            systemUserQueryWrapper.like("nickname", req.getKeyword())
                    .or(q -> q.like("phone_number", req.getKeyword()));
        }
        if (NumUtils.isNonnegativeNumber(req.getStatus())) {
            systemUserQueryWrapper.eq("status", req.getStatus());
        }
        systemUserQueryWrapper.orderByAsc("create_time");

        Page<SystemUser> page = systemUserService.page(req.getPage(), systemUserQueryWrapper);
        SystemUserListResp resp = new SystemUserListResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }

    @GetMapping("{userId}")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @SysLog(operationGroup = "系统用户", operationName = "查询用户信息")
    public RetResult<SystemUserResp> getSystemUser(@PathVariable String userId) {
        Assert.notNull(userId, "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        return RetMarker.makeSuccessRsp(new SystemUserResp(systemUser));
    }

    @PostMapping("modify/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @SysLog(operationGroup = "系统用户", operationName = "更改用户信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String userId, @RequestBody SingleFieldModifyReq req) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");
        Assert.hasText(req.getField(), "参数错误");

        UpdateWrapper<SystemUser> systemUserUpdateWrapper = new UpdateWrapper<>();
        systemUserUpdateWrapper.eq("id", userId);

        switch (req.getField()) {
            case "nickname" -> systemUserUpdateWrapper.set("nickname", req.getValue());
            case "status" -> {
                Assert.isTrue(SystemUserStatus.valid(Integer.parseInt(req.getValue())), "参数错误");
                systemUserUpdateWrapper.set("status", req.getValue());
            }
            case "phoneNumber" -> systemUserUpdateWrapper.set("phone_number", req.getValue());
            default -> {
                return RetMarker.makeErrRsp("参数错误");
            }
        }

        Assert.isTrue(systemUserService.update(systemUserUpdateWrapper), "修改失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:user:add')")
    @SysLog(operationGroup = "系统用户", operationName = "新增用户", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemUserUpdateReq req) {
        Assert.notNull(req, "参数错误");

        SystemUser systemUser = new SystemUser(
                req.getNickname(),
                req.getStatus(),
                req.getPhoneNumber(),
                null,
                null
        );
        systemUser.setCreateBy(getLoginManagerId());
        systemUser.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(systemUser);

        Assert.isTrue(systemUserService.save(systemUser), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @SysLog(operationGroup = "系统用户", operationName = "修改用户", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String userId, @RequestBody @Validated(ValidationGroup.Update.class) SystemUserUpdateReq req) {
        Assert.hasText(userId, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        UpdateWrapper<SystemUser> updateWrapper = UpdateConditionBuilder.build(systemUser, req, getLoginManagerId());
        UniqueDetectionTool.check(systemUser);
        Assert.isTrue(systemUserService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:remove')")
    @SysLog(operationGroup = "系统用户", operationName = "删除用户", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String userId) {
        Assert.hasText(userId, "参数错误");
        Assert.isTrue(!"1".equalsIgnoreCase(userId), "该用户不支持删除");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统角色信息不存在");

        systemUserService.removeById(systemUser);

        // 删除角色的第三方账号绑定关系
        QueryWrapper<SystemUserRelation> systemUserRelationQueryWrapper = new QueryWrapper<>();
        systemUserRelationQueryWrapper.eq("system_user_id", systemUser.getId());
        systemUserRelationService.remove(systemUserRelationQueryWrapper);

        return RetMarker.makeSuccessRsp();
    }

}
