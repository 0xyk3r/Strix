package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRoleMenu;
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
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
 * @author 安炯奕
 * @date 2021/6/18 23:41
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

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统菜单", operationName = "查询菜单列表")
    public RetResult<SystemMenuListResp> getSystemMenuList() {
        List<SystemMenu> systemMenuList = systemMenuService.list();
        List<SystemPermission> systemPermissionList = systemPermissionService.list();

        return RetMarker.makeSuccessRsp(new SystemMenuListResp(systemMenuList, systemPermissionList));
    }

    @GetMapping("{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu')")
    @StrixLog(operationGroup = "系统菜单", operationName = "查询菜单信息")
    public RetResult<SystemMenuResp> getSystemMenu(@PathVariable String menuId) {
        Assert.notNull(menuId, "参数错误");
        SystemMenu sm = systemMenuService.getById(menuId);
        Assert.notNull(sm, "系统菜单信息不存在");

        return RetMarker.makeSuccessRsp(new SystemMenuResp(sm.getId(), sm.getKey(), sm.getName(), sm.getUrl(), sm.getIcon(), sm.getParentId(), sm.getSortValue()));
    }

    @PostMapping("modify/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "更改菜单信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String menuId, @RequestBody SingleFieldModifyReq req) {
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统人员信息不存在");
        Assert.hasText(req.getField(), "参数错误");

        UpdateWrapper<SystemMenu> systemMenuUpdateWrapper = new UpdateWrapper<>();
        systemMenuUpdateWrapper.eq("id", menuId);

        if ("icon".equals(req.getField())) {
            systemMenuUpdateWrapper.set("icon", req.getValue());
        } else {
            return RetMarker.makeErrRsp("参数错误");
        }
        Assert.isTrue(systemMenuService.update(systemMenuUpdateWrapper), "修改失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

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
        systemMenu.setCreateBy(getLoginManagerId());
        systemMenu.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(systemMenu);

        Assert.isTrue(systemMenuService.save(systemMenu), "保存失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    @StrixLog(operationGroup = "系统菜单", operationName = "修改菜单", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String menuId, @RequestBody @Validated(UpdateGroup.class) SystemMenuUpdateReq req) {
        Assert.hasText(menuId, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统菜单信息不存在");

        UpdateWrapper<SystemMenu> updateWrapper = UpdateConditionBuilder.build(systemMenu, req, getLoginManagerId());
        UniqueDetectionTool.check(systemMenu);
        Assert.isTrue(systemMenuService.update(updateWrapper), "保存失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();
        // 刷新 redis 中的登录用户信息
        SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
        systemManagerService.refreshLoginInfoByMenu(menuId);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{menuId}")
    @PreAuthorize("@ss.hasPermission('system:menu:remove')")
    @StrixLog(operationGroup = "系统菜单", operationName = "删除菜单", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String menuId) {
        Assert.hasText(menuId, "参数错误");
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统菜单信息不存在");

        // 查找子菜单
        List<SystemMenu> systemMenuList = systemMenuService.list();
        Set<String> childrenMenusIdList = findSystemMenuChildrenIdList(systemMenuList, menuId);

        // 批量删除菜单
        systemMenuService.removeByIds(childrenMenusIdList);
        // 删除角色和菜单间关系
        systemRoleMenuService.remove(
                new LambdaQueryWrapper<SystemRoleMenu>()
                        .in(SystemRoleMenu::getSystemMenuId, childrenMenusIdList)
        );
        // 删除菜单对应的权限
        systemPermissionService.remove(
                new LambdaQueryWrapper<SystemPermission>()
                        .in(SystemPermission::getMenuId, childrenMenusIdList)
        );

        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getSystemMenuTree() {
        return RetMarker.makeSuccessRsp(systemMenuService.getTreeData());
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
