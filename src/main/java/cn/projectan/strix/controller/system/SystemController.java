package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.login.SystemLoginReq;
import cn.projectan.strix.model.response.system.SystemMenuResp;
import cn.projectan.strix.model.response.system.login.SystemManagerLoginResp;
import cn.projectan.strix.service.system.SystemLoginService;
import cn.projectan.strix.service.system.SystemMenuService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.system.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 系统基础接口
 *
 * @author ProjectAn
 * @since 2021/5/12 18:39
 */
@Slf4j
@RestController
@RequestMapping("system")
@RequiredArgsConstructor
@Tag(name = "系统 - 认证")
public class SystemController extends BaseSystemController {

    private final SystemMenuService systemMenusService;
    private final SystemLoginService systemLoginService;

    /**
     * 系统登录
     */
    @Operation(summary = "管理员登录")
    @Anonymous
    @PostMapping("login")
    @StrixLog(operationGroup = "系统登录", operationName = "系统登录", operationType = SystemLogOperType.LOGIN)
    public RetResult<SystemManagerLoginResp> login(@RequestBody @Validated SystemLoginReq req) {
        return systemLoginService.login(req);
    }

    /**
     * 重新获取Token
     */
    @Operation(summary = "续期 Token")
    @PostMapping("renewToken")
    public RetResult<SystemManagerLoginResp> renewToken() {
        return systemLoginService.renewToken(loginManagerId());
    }

    /**
     * 获取系统菜单
     */
    @Operation(summary = "获取菜单列表")
    @GetMapping("menus")
    public RetResult<SystemMenuResp> getMenuList() {
        List<String> systemMenuKeys = Optional.ofNullable(SecurityUtil.getSystemManagerLoginInfo()).map(LoginSystemManager::getMenusKeys).orElse(null);
        Assert.notEmpty(systemMenuKeys, I18nUtil.get("assert.menu.noPermission"));

        List<SystemMenu> systemMenus = systemMenusService.listByKeys(systemMenuKeys);
        Assert.notEmpty(systemMenus, I18nUtil.get("assert.menu.noAvailablePermission"));

        return RetBuilder.success(new SystemMenuResp(systemMenus));
    }

}
