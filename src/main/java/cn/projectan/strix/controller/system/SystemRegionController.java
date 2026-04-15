package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.listener.StrixCommonListener;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemRegion;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.region.SystemRegionListReq;
import cn.projectan.strix.model.request.system.region.SystemRegionUpdateReq;
import cn.projectan.strix.model.response.common.CommonCascaderDataResp;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.region.SystemRegionChildrenListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionResp;
import cn.projectan.strix.model.event.cache.RegionChangedEvent;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemRegionService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统地区
 *
 * @author ProjectAn
 * @since 2021/9/29 17:45
 */
@Slf4j
@RestController
@RequestMapping("system/region")
@RequiredArgsConstructor
@Tag(name = "系统 - 地区管理")
public class SystemRegionController extends BaseSystemController {

    private final SystemRegionService systemRegionService;
    private final SystemManagerService systemManagerService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<StrixCommonListener> strixCommonListenerProvider;

    /**
     * 获取地区列表
     */
    @Operation(summary = "地区列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区列表")
    public RetResult<SystemRegionListResp> getSystemRegionList(SystemRegionListReq req) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        int maxRegionLevel = 0;
        if (notSuperManager()) {
            maxRegionLevel = systemRegionService.getMaxRegionLevel(loginManagerRegionPermissions);
        }

        Page<SystemRegion> page = systemRegionService.listPage(req, loginManagerRegionPermissions, isSuperManager(), maxRegionLevel);

        SystemRegionListResp resp = new SystemRegionListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 获取地区信息
     */
    @Operation(summary = "地区详情")
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区信息")
    public RetResult<SystemRegionResp> getSystemRegion(@Parameter(description = "地区 ID") @PathVariable String id) {
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, I18nUtil.notFound("field.systemRegion"));
        checkLoginManagerRegionPermission(id);

        return RetBuilder.success(new SystemRegionResp(systemRegion.getId(), systemRegion.getName(), systemRegion.getLevel(), systemRegion.getParentId(), systemRegion.getFullPath(), systemRegion.getFullName(), systemRegion.getRemarks()));
    }

    /**
     * 获取地区子节点
     */
    @Operation(summary = "获取子级地区")
    @GetMapping("{id}/children")
    @PreAuthorize("@ss.hasPermission('system:region')")
    public RetResult<SystemRegionChildrenListResp> getSystemRegionChildren(@Parameter(description = "父级地区 ID") @PathVariable String id) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, I18nUtil.notFound("field.systemRegion"));
        checkLoginManagerRegionPermission(id);

        List<SystemRegion> childrenList = systemRegionService.listByParentId(systemRegion.getId(), loginManagerRegionPermissions);

        return RetBuilder.success(new SystemRegionChildrenListResp(childrenList));
    }

    /**
     * 新增地区
     */
    @Operation(summary = "新增地区")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:region:add')")
    @StrixLog(operationGroup = "系统地区", operationName = "新增地区", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        resolveParentId(req);
        checkLoginManagerRegionPermission(req.getParentId());

        SystemRegion systemRegion = new SystemRegion(
                req.getName(),
                (short) 0,
                req.getParentId(),
                null,
                null,
                req.getRemarks()
        );

        UniqueChecker.check(systemRegion);
        Assert.isTrue(systemRegionService.save(systemRegion), "保存失败");

        Assert.isTrue(systemRegionService.updateFullInfo(systemRegion.getId()), "处理信息失败");

        eventPublisher.publishEvent(new RegionChangedEvent(this, List.of(systemRegion.getId(), systemRegion.getParentId())));

        return RetBuilder.success();
    }

    /**
     * 修改地区
     */
    @Operation(summary = "编辑地区")
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @StrixLog(operationGroup = "系统地区", operationName = "修改地区", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "地区 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, I18nUtil.notFound("field.systemRegion"));
        checkLoginManagerRegionPermission(id);

        resolveParentId(req);
        if (!"0".equals(req.getParentId())) {
            checkLoginManagerRegionPermission(req.getParentId());
        }

        // 保存旧数据用于更新子节点
        String oldName = systemRegion.getName();
        String oldFullPath = systemRegion.getFullPath();
        String oldFullName = systemRegion.getFullName();
        String oldParentId = systemRegion.getParentId();
        String oldRemarks = systemRegion.getRemarks();

        boolean nameChanged = !oldName.equals(req.getName());
        boolean parentChanged = !oldParentId.equals(req.getParentId());
        boolean remarksChanged = !java.util.Objects.equals(oldRemarks, req.getRemarks());

        // 检查唯一性约束
        systemRegion.setName(req.getName());
        systemRegion.setParentId(req.getParentId());
        systemRegion.setRemarks(req.getRemarks());
        UniqueChecker.check(systemRegion);

        // 恢复旧数据，让 Service 层处理更新逻辑
        systemRegion.setName(oldName);
        systemRegion.setParentId(oldParentId);
        systemRegion.setFullPath(oldFullPath);
        systemRegion.setFullName(oldFullName);
        systemRegion.setRemarks(oldRemarks);

        if (parentChanged) {
            // 父节点变更：需要更新自身及子节点的 fullPath, fullName, level
            // 如果同时改了名称，先处理父节点变更（使用新名称）
            if (nameChanged) {
                systemRegion.setName(req.getName());
            }
            systemRegionService.updateRelevantRegion(systemRegion, oldFullPath, oldFullName, req.getParentId());
            // 如果还有备注变更，单独更新
            if (remarksChanged) {
                systemRegion.setRemarks(req.getRemarks());
                systemRegionService.updateBasicInfo(systemRegion);
            }
        } else if (nameChanged) {
            // 仅名称变更：需要更新自身及子节点的 fullName
            systemRegionService.updateRegionName(systemRegion, req.getName());
            // 如果还有备注变更，单独更新
            if (remarksChanged) {
                systemRegion.setRemarks(req.getRemarks());
                systemRegionService.updateBasicInfo(systemRegion);
            }
        } else if (remarksChanged) {
            // 仅更新基本信息（如备注）
            systemRegion.setRemarks(req.getRemarks());
            systemRegionService.updateBasicInfo(systemRegion);
        }

        return RetBuilder.success();
    }

    /**
     * 删除地区
     */
    @Operation(summary = "删除地区")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:remove')")
    @StrixLog(operationGroup = "系统地区", operationName = "删除地区", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "地区 ID") @PathVariable String id) {
        checkLoginManagerRegionPermission(id);

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, I18nUtil.notFound("field.systemRegion"));

        List<String> removeIdList = systemRegionService.listIdsByFullPath(systemRegion.getFullPath());

        // 批量删除
        systemRegionService.removeByIds(removeIdList);
        // 删除管理人员的地区权限关系
        systemManagerService.clearRegionId(removeIdList);

        List<String> regionIds = new java.util.ArrayList<>(removeIdList);
        regionIds.add(systemRegion.getParentId());
        eventPublisher.publishEvent(new RegionChangedEvent(this, regionIds));

        // 循环处理后续工作
        for (String removeId : removeIdList) {
            strixCommonListenerProvider.ifAvailable(listener -> listener.deleteSystemRegionNotify(removeId));
        }

        return RetBuilder.success();
    }

    /**
     * 获取地区级联数据
     */
    @Operation(summary = "获取级联选择器数据")
    @GetMapping("cascader")
    public RetResult<CommonCascaderDataResp> getCascaderData() {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        List<SystemRegion> systemRegionList = systemRegionService.listByRegionPermissions(loginManagerRegionPermissions);
        return RetBuilder.success(new CommonCascaderDataResp(systemRegionList));
    }

    /**
     * 获取地区树形数据
     */
    @Operation(summary = "获取树形数据")
    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getTreeData() {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        List<SystemRegion> systemRegionList = systemRegionService.listByRegionPermissions(loginManagerRegionPermissions);
        return RetBuilder.success(new CommonTreeDataResp(systemRegionList));
    }

    /**
     * 解析并设置 parentId（提取自 新增/修改 的公共逻辑）
     */
    private void resolveParentId(SystemRegionUpdateReq req) {
        if (!StringUtils.hasText(req.getParentId())) {
            if (notSuperManager()) {
                Assert.hasText(loginManagerRegionId(), "当前登录管理员无地区权限，无法操作地区");
                req.setParentId(loginManagerRegionId());
            } else {
                req.setParentId(SystemRegionService.ROOT_PARENT_ID);
            }
        }
    }

}
