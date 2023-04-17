package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.constant.SystemUserStatus;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.SystemUserRelation;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.systemuser.SystemUserListQueryReq;
import cn.projectan.strix.model.request.system.systemuser.SystemUserUpdateReq;
import cn.projectan.strix.model.response.system.systemuser.SystemUserListQueryResp;
import cn.projectan.strix.model.response.system.systemuser.SystemUserQueryByIdResp;
import cn.projectan.strix.service.SystemUserRelationService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.NumUtils;
import cn.projectan.strix.utils.StrixAssert;
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
    @PreAuthorize("@ss.hasRead('System_User')")
    public RetResult<SystemUserListQueryResp> getSystemUserList(SystemUserListQueryReq systemUserListQueryReq) {
        QueryWrapper<SystemUser> systemUserQueryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(systemUserListQueryReq.getKeyword())) {
            systemUserQueryWrapper.like("nickname", systemUserListQueryReq.getKeyword())
                    .or(q -> q.like("phone_number", systemUserListQueryReq.getKeyword()));
        }
        if (NumUtils.isNonnegativeNumber(systemUserListQueryReq.getStatus())) {
            systemUserQueryWrapper.eq("status", systemUserListQueryReq.getStatus());
        }
        systemUserQueryWrapper.orderByAsc("create_time");

        Page<SystemUser> page = systemUserService.page(systemUserListQueryReq.getPage(), systemUserQueryWrapper);
        SystemUserListQueryResp resp = new SystemUserListQueryResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }

    @GetMapping("{userId}")
    @PreAuthorize("@ss.hasRead('System_User')")
    public RetResult<SystemUserQueryByIdResp> getSystemUser(@PathVariable String userId) {
        Assert.notNull(userId, "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        return RetMarker.makeSuccessRsp(new SystemUserQueryByIdResp(systemUser));
    }

    @PostMapping("modify/{userId}")
    @PreAuthorize("@ss.hasWrite('System_User')")
    public RetResult<Object> modifyField(@PathVariable String userId, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");

        UpdateWrapper<SystemUser> systemUserUpdateWrapper = new UpdateWrapper<>();
        systemUserUpdateWrapper.eq("id", userId);

        switch (singleFieldModifyReq.getField()) {
            case "nickname":
                systemUserUpdateWrapper.set("nickname", singleFieldModifyReq.getValue());
                break;
            case "status":
                StrixAssert.in(singleFieldModifyReq.getValue(), "参数错误", SystemUserStatus.BANNED, SystemUserStatus.NORMAL);
                systemUserUpdateWrapper.set("status", singleFieldModifyReq.getValue());
                break;
            case "phoneNumber":
                systemUserUpdateWrapper.set("phone_number", singleFieldModifyReq.getValue());
                break;
            default:
                return RetMarker.makeErrRsp("参数错误");
        }

        Assert.isTrue(systemUserService.update(systemUserUpdateWrapper), "修改失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasWrite('System_User')")
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemUserUpdateReq systemUserUpdateReq) {
        Assert.notNull(systemUserUpdateReq, "参数错误");

        SystemUser systemUser = new SystemUser(
                systemUserUpdateReq.getNickname(),
                systemUserUpdateReq.getStatus(),
                systemUserUpdateReq.getPhoneNumber(),
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
    @PreAuthorize("@ss.hasWrite('System_User')")
    public RetResult<Object> update(@PathVariable String userId, @RequestBody @Validated(ValidationGroup.Update.class) SystemUserUpdateReq systemUserUpdateReq) {
        Assert.hasText(userId, "参数错误");
        Assert.notNull(systemUserUpdateReq, "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "系统用户信息不存在");

        UpdateWrapper<SystemUser> updateWrapper = UpdateConditionBuilder.build(systemUser, systemUserUpdateReq, getLoginManagerId());
        UniqueDetectionTool.check(systemUser);
        Assert.isTrue(systemUserService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{userId}")
    @PreAuthorize("@ss.hasWrite('System_User')")
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
