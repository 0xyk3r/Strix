package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.db.system.SystemPermission;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.event.cache.MenuChangedEvent;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.menu.SystemMenuUpdateReq;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuResp;
import cn.projectan.strix.service.system.SystemMenuService;
import cn.projectan.strix.service.system.SystemPermissionService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统菜单
 *
 * @author ProjectAn
 * @since 2021/6/18 23:41
 */
@Slf4j
@RestController
@RequestMapping("system/menu")
@RequiredArgsConstructor
@Tag(name = "系统 - 菜单管理")
public class SystemMenuController extends BaseSystemController {

    private final SystemMenuService systemMenuService;
    private final SystemPermissionService systemPermissionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询菜单列表
     */
    @Operation(summary = "菜单列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统菜单", operationName = "查询菜单列表")
    public RetResult<SystemMenuListResp> getSystemMenuList() {
        List<SystemMenu> systemMenuList = systemMenuService.list();
        List<SystemPermission> systemPermissionList = systemPermissionService.list();

        return RetBuilder.success(new SystemMenuListResp(systemMenuList, systemPermissionList));
    }

    /**
     * 查询菜单信息
     */
    @Operation(summary = "菜单详情")
    @GetMapping("{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统菜单", operationName = "查询菜单信息")
    public RetResult<SystemMenuResp> getSystemMenu(@Parameter(description = "菜单 ID") @PathVariable String menuId) {
        SystemMenu sm = systemMenuService.getById(menuId);
        Assert.notNull(sm, I18nUtil.notFound("field.systemMenu"));

        return RetBuilder.success(new SystemMenuResp(sm.getId(), sm.getKey(), sm.getName(), sm.getUrl(), sm.getIcon(), sm.getParentId(), sm.getSortValue()));
    }

    /**
     * 更改菜单信息
     */
    @Operation(summary = "修改菜单字段")
    @PostMapping("modify/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "更改菜单信息", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> modifyField(@Parameter(description = "菜单 ID") @PathVariable String menuId, @RequestBody @Validated SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        Assert.isTrue("icon".equals(req.getField()), "参数错误");

        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, I18nUtil.notFound("field.systemMenu"));

        Assert.isTrue(systemMenuService.updateIcon(menuId, req.getValue()), "修改失败");
        eventPublisher.publishEvent(new MenuChangedEvent(this));

        return RetBuilder.success();
    }

    /**
     * 新增菜单
     */
    @Operation(summary = "新增菜单")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:menu:add')")
    @StrixLog(operationGroup = "系统菜单", operationName = "新增菜单", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemMenuUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));

        SystemMenu systemMenu = new SystemMenu(
                req.getKey(),
                req.getName(),
                req.getUrl(),
                req.getIcon(),
                req.getParentId(),
                req.getSortValue()
        );

        UniqueChecker.check(systemMenu);

        Assert.isTrue(systemMenuService.save(systemMenu), "保存失败");
        eventPublisher.publishEvent(new MenuChangedEvent(this));

        return RetBuilder.success();
    }

    /**
     * 修改菜单
     */
    @Operation(summary = "编辑菜单")
    @PostMapping("update/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "修改菜单", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "菜单 ID") @PathVariable String menuId, @RequestBody @Validated(UpdateGroup.class) SystemMenuUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, I18nUtil.notFound("field.systemMenu"));

        LambdaUpdateWrapper<SystemMenu> updateWrapper = UpdateBuilder.build(systemMenu, req);
        UniqueChecker.check(systemMenu);
        Assert.isTrue(systemMenuService.update(updateWrapper), "保存失败");
        eventPublisher.publishEvent(new MenuChangedEvent(this));

        return RetBuilder.success();
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单")
    @PostMapping("remove/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:remove')")
    @StrixLog(operationGroup = "系统菜单", operationName = "删除菜单", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "菜单 ID") @PathVariable String menuId) {
        systemMenuService.deleteByIds(List.of(menuId));
        return RetBuilder.success();
    }

    /**
     * 获取菜单树
     */
    @Operation(summary = "获取菜单树")
    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getSystemMenuTree() {
        return RetBuilder.success(systemMenuService.getTreeData());
    }

}
