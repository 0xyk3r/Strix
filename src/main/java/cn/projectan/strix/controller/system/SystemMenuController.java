package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ramcache.SystemMenuCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.NeedSystemPermission;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemRoleMenu;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.systemmenu.SystemMenuUpdateReq;
import cn.projectan.strix.model.response.system.systemmenu.SystemMenuListQueryResp;
import cn.projectan.strix.model.response.system.systemmenu.SystemMenuQueryByIdResp;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.service.SystemRoleMenuService;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/6/18 23:41
 */
@Slf4j
@RestController
@RequestMapping("system/menu")
public class SystemMenuController extends BaseSystemController {

    @Autowired
    private SystemMenuService systemMenuService;
    @Autowired
    private SystemRoleMenuService systemRoleMenuService;

    @Autowired
    private SystemMenuCache systemMenuCache;

    @GetMapping("")
    @NeedSystemPermission("System_Menu")
    public RetResult<SystemMenuListQueryResp> getSystemMenuList() {
        List<SystemMenu> systemMenuList = systemMenuService.list();

        return RetMarker.makeSuccessRsp(new SystemMenuListQueryResp(systemMenuList));
    }

    @GetMapping("{menuId}")
    @NeedSystemPermission("System_Menu")
    public RetResult<SystemMenuQueryByIdResp> getSystemMenu(@PathVariable String menuId) {
        Assert.notNull(menuId, "参数错误");
        SystemMenu sm = systemMenuService.getById(menuId);
        Assert.notNull(sm, "系统菜单信息不存在");

        return RetMarker.makeSuccessRsp(new SystemMenuQueryByIdResp(sm.getId(), sm.getName(), sm.getUrl(), sm.getIcon(), sm.getParentId(), sm.getSortValue()));
    }

    @PostMapping("modify/{menuId}")
    @NeedSystemPermission(value = "System_Menu", isEdit = true)
    public RetResult<Object> modifyField(@PathVariable String menuId, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统人员信息不存在");
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");

        UpdateWrapper<SystemMenu> systemMenuUpdateWrapper = new UpdateWrapper<>();
        systemMenuUpdateWrapper.eq("id", menuId);

        switch (singleFieldModifyReq.getField()) {
            case "icon":
                systemMenuUpdateWrapper.set("icon", singleFieldModifyReq.getValue());
                break;
            default:
                return RetMarker.makeErrRsp("参数错误");
        }
        Assert.isTrue(systemMenuService.update(systemMenuUpdateWrapper), "修改失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @NeedSystemPermission(value = "System_Menu", isEdit = true)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemMenuUpdateReq systemMenuUpdateReq) {
        Assert.notNull(systemMenuUpdateReq, "参数错误");

        SystemMenu systemMenu = new SystemMenu(
                systemMenuUpdateReq.getName(),
                systemMenuUpdateReq.getUrl(),
                systemMenuUpdateReq.getIcon(),
                systemMenuUpdateReq.getParentId(),
                systemMenuUpdateReq.getSortValue()
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
    @NeedSystemPermission(value = "System_Menu", isEdit = true)
    public RetResult<Object> update(@PathVariable String menuId, @RequestBody @Validated(ValidationGroup.Update.class) SystemMenuUpdateReq systemMenuUpdateReq) {
        Assert.hasText(menuId, "参数错误");
        Assert.notNull(systemMenuUpdateReq, "参数错误");
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统菜单信息不存在");

        UpdateWrapper<SystemMenu> updateWrapper = UpdateConditionBuilder.build(systemMenu, systemMenuUpdateReq, getLoginManagerId());
        UniqueDetectionTool.check(systemMenu);
        Assert.isTrue(systemMenuService.update(updateWrapper), "保存失败");
        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{menuId}")
    @NeedSystemPermission(value = "System_Menu", isEdit = true)
    public RetResult<Object> remove(@PathVariable String menuId) {
        Assert.hasText(menuId, "参数错误");
        SystemMenu systemMenu = systemMenuService.getById(menuId);
        Assert.notNull(systemMenu, "系统菜单信息不存在");
        // TODO 权限验证
        systemMenuService.removeById(systemMenu);

        // 删除角色和菜单间关系
        QueryWrapper<SystemRoleMenu> deleteRoleMenuRelationQueryWrapper = new QueryWrapper<>();
        deleteRoleMenuRelationQueryWrapper.eq("system_menu_id", systemMenu.getId());
        systemRoleMenuService.remove(deleteRoleMenuRelationQueryWrapper);

        // 更新缓存
        systemMenuCache.updateRamAndRedis();

        return RetMarker.makeSuccessRsp();
    }

}
