package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.db.system.DictGroup;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.dict.DictGroupUpdateReq;
import cn.projectan.strix.model.response.system.dict.DictGroupListResp;
import cn.projectan.strix.service.system.DictGroupService;
import cn.projectan.strix.service.system.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典分组管理
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Slf4j
@RestController
@RequestMapping("system/dict-group")
@RequiredArgsConstructor
@Tag(name = "系统 - 字典分组管理")
public class SystemDictGroupController extends BaseSystemController {

    private final DictGroupService dictGroupService;
    private final DictService dictService;

    @Operation(summary = "分组列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:dict')")
    @StrixLog(operationGroup = "字典分组", operationName = "查询分组列表")
    public RetResult<DictGroupListResp> list() {
        List<DictGroup> groups = dictGroupService.listAll();
        List<Long> counts = new ArrayList<>();
        for (DictGroup g : groups) {
            long count = dictService.lambdaQuery().eq(Dict::getGroupId, g.getId()).count();
            counts.add(count);
        }
        return RetBuilder.success(new DictGroupListResp(groups, counts));
    }

    @Operation(summary = "新增分组")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:dict:add')")
    @StrixLog(operationGroup = "字典分组", operationName = "新增分组", operationType = SystemLogOperType.ADD)
    public RetResult<Void> add(@RequestBody @Validated(InsertGroup.class) DictGroupUpdateReq req) {
        dictGroupService.saveGroup(req);
        return RetBuilder.success();
    }

    @Operation(summary = "修改分组")
    @PostMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @StrixLog(operationGroup = "字典分组", operationName = "修改分组", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) DictGroupUpdateReq req) {
        dictGroupService.updateGroup(id, req);
        return RetBuilder.success();
    }

    @Operation(summary = "删除分组")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:remove')")
    @StrixLog(operationGroup = "字典分组", operationName = "删除分组", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> delete(@PathVariable String id) {
        long refCount = dictService.lambdaQuery().eq(Dict::getGroupId, id).count();
        Assert.isTrue(refCount == 0, "该分组下仍有 " + refCount + " 个字典，不可删除");
        dictGroupService.deleteGroup(id);
        return RetBuilder.success();
    }

}
