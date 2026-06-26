package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.profile.ProfileAvatarUpdateReq;
import cn.projectan.strix.model.request.system.profile.ProfileNicknameUpdateReq;
import cn.projectan.strix.model.request.system.profile.ProfilePasswordUpdateReq;
import cn.projectan.strix.model.response.system.profile.ProfileLoginLogResp;
import cn.projectan.strix.service.system.SystemLogService;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.crypto.StrixSM3Util;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心接口（操作当前登录人自身信息，无需管理权限）
 *
 * @author ProjectAn
 */
@RestController
@RequestMapping("system/profile")
@RequiredArgsConstructor
@Tag(name = "系统 - 个人中心")
public class ProfileController extends BaseSystemController {

    private final SystemManagerService systemManagerService;
    private final SystemLogService systemLogService;

    /**
     * 获取当前登录人信息
     */
    @Operation(summary = "获取个人信息")
    @GetMapping("")
    @StrixLog(operationGroup = "个人中心", operationName = "获取个人信息")
    public RetResult<SystemManager> getProfile() {
        SystemManager manager = systemManagerService.getById(loginManagerId());
        Assert.notNull(manager, I18nUtil.notFound("field.systemManager"));
        // 清除密码字段
        manager.setLoginPassword(null);
        return RetBuilder.success(manager);
    }

    /**
     * 修改昵称
     */
    @Operation(summary = "修改昵称")
    @PostMapping("nickname")
    @StrixLog(operationGroup = "个人中心", operationName = "修改昵称", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> updateNickname(@RequestBody @Validated ProfileNicknameUpdateReq req) {
        String managerId = loginManagerId();
        SystemManager manager = systemManagerService.getById(managerId);
        Assert.notNull(manager, I18nUtil.notFound("field.systemManager"));

        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getId, managerId)
                .set(SystemManager::getNickname, req.getNickname())
                .update();

        return RetBuilder.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("password")
    @StrixLog(operationGroup = "个人中心", operationName = "修改密码", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> updatePassword(@RequestBody @Validated ProfilePasswordUpdateReq req) {
        String managerId = loginManagerId();
        SystemManager manager = systemManagerService.getById(managerId);
        Assert.notNull(manager, I18nUtil.notFound("field.systemManager"));

        Assert.isTrue(
                StrixSM3Util.matches(req.getOldPassword(), manager.getLoginPassword()),
                "当前密码不正确"
        );

        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getId, managerId)
                .set(SystemManager::getLoginPassword, StrixSM3Util.hashPassword(req.getNewPassword()))
                .update();

        return RetBuilder.success();
    }

    /**
     * 更新头像配置
     */
    @Operation(summary = "更新头像配置")
    @PostMapping("avatar")
    @StrixLog(operationGroup = "个人中心", operationName = "更新头像", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> updateAvatar(@RequestBody @Validated ProfileAvatarUpdateReq req) {
        String managerId = loginManagerId();
        SystemManager manager = systemManagerService.getById(managerId);
        Assert.notNull(manager, I18nUtil.notFound("field.systemManager"));

        String avatarConfig = StringUtils.hasText(req.getAvatarConfig()) ? req.getAvatarConfig() : null;

        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getId, managerId)
                .set(SystemManager::getAvatarConfig, avatarConfig)
                .update();

        return RetBuilder.success();
    }

    /**
     * 获取个人登录记录（仅查询当前登录人的登录日志）
     */
    @Operation(summary = "获取个人登录记录")
    @GetMapping("login-logs")
    @StrixLog(operationGroup = "个人中心", operationName = "查询登录记录")
    public RetResult<ProfileLoginLogResp> getLoginLogs(BasePageReq<SystemLog> pageReq) {
        String managerId = loginManagerId();

        Page<SystemLog> page = systemLogService.lambdaQuery()
                .eq(SystemLog::getClientUser, managerId)
                .eq(SystemLog::getOperationType, SystemLogOperType.LOGIN)
                .orderByDesc(SystemLog::getOperationTime)
                .page(pageReq.getPage());

        return RetBuilder.success(new ProfileLoginLogResp(page.getRecords(), page.getTotal()));
    }

}
