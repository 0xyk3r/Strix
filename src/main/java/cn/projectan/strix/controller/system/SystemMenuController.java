package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.menu.SystemMenuUpdateReq;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.menu.SystemMenuResp;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.service.SystemRoleMenuService;
import cn.projectan.strix.util.SpringUtil;
import cn.projectan.strix.util.UniqueChecker;
import cn.projectan.strix.util.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class SystemMenuController extends BaseSystemController {

    private final SystemMenuService systemMenuService;
    private final SystemRoleMenuService systemRoleMenuService;
    private final SystemPermissionService systemPermissionService;
    private final SystemMenuCache systemMenuCache;

    /**
     * 查询菜单列表
     */
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
    @GetMapping("{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统菜单", operationName = "查询菜单信息")
    public RetResult<SystemMenuResp> getSystemMenu(@PathVariable String menuId) {
        SystemMenu sm = systemMenuService.getById(menuId);
        Assert.notNull(sm, "系统菜单信息不存在");

        return RetBuilder.success(new SystemMenuResp(sm.getId(), sm.getKey(), sm.getName(), sm.getUrl(), sm.getIcon(), sm.getParentId(), sm.getSortValue()));
    }

    /**
     * 更改菜单信息
     */
    @PostMapping("modify/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "更改菜单信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String menuId, @RequestBody SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        Assert.isTrue("icon".equals(req.getField()), "参数错误");

        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统人员信息不存在");

        Assert.isTrue(
                systemMenuService.lambdaUpdate()
                        .eq(SystemMenu::getId, menuId)
                        .set(SystemMenu::getIcon, req.getValue())
                        .update(),
                "修改失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetBuilder.success();
    }

    /**
     * 新增菜单
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:menu:add')")
    @StrixLog(operationGroup = "系统菜单", operationName = "新增菜单", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemMenuUpdateReq req) {
        Assert.notNull(req, "参数错误");

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
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetBuilder.success();
    }

    /**
     * 修改菜单
     */
    @PostMapping("update/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "修改菜单", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String menuId, @RequestBody @Validated(UpdateGroup.class) SystemMenuUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统菜单信息不存在");

        LambdaUpdateWrapper<SystemMenu> updateWrapper = UpdateBuilder.build(systemMenu, req);
        UniqueChecker.check(systemMenu);
        Assert.isTrue(systemMenuService.update(updateWrapper), "保存失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByMenu(menuId);

        return RetBuilder.success();
    }

    /**
     * 删除菜单
     */
    @PostMapping("remove/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:remove')")
    @StrixLog(operationGroup = "系统菜单", operationName = "删除菜单", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String menuId) {
        systemMenuService.deleteByIds(List.of(menuId));
        return RetBuilder.success();
    }

    /**
     * 获取菜单树
     */
    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getSystemMenuTree() {
        return RetBuilder.success(systemMenuService.getTreeData());
    }

    private Set<String> findSystemMenuChildrenIdList(List<SystemMenu> menus, String parentId) {
        List<String> menuIds = new ArrayList<>();
        menuIds.add(parentId);

        SystemMenu parentSystemMenu = menus.stream().filter(m -> m.getId().equals(parentId)).findFirst().orElse(null);
        if (parentSystemMenu == null) {
            return null;
        }

        List<String> subMenuIds = menus.stream().filter(m -> m.getParentId().equals(parentId)).map(SystemMenu::getId).toList();
        for (String subMenuId : subMenuIds) {
            Set<String> systemMenuChildrenIdList = findSystemMenuChildrenIdList(menus, subMenuId);
            if (systemMenuChildrenIdList != null) {
                menuIds.addAll(systemMenuChildrenIdList);
            }
        }
        return new HashSet<>(menuIds);
    }

}
