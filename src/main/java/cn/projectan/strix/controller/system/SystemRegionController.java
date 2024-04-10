package cn.projectan.strix.controller.system;

import cn.hutool.core.util.ObjectUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemRegionCache;
import cn.projectan.strix.core.listener.StrixCommonListener;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.region.SystemRegionListReq;
import cn.projectan.strix.model.request.system.region.SystemRegionUpdateReq;
import cn.projectan.strix.model.response.common.CommonCascaderDataResp;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.region.SystemRegionChildrenListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionResp;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @date 2021/9/29 17:45
 */
@Slf4j
@RestController
@RequestMapping("system/region")
public class SystemRegionController extends BaseSystemController {

    private final SystemRegionService systemRegionService;
    private final SystemManagerService systemManagerService;
    private final SystemRegionCache systemRegionCache;
    private final StrixCommonListener strixCommonListener;

    @Autowired
    public SystemRegionController(SystemRegionService systemRegionService, SystemManagerService systemManagerService, SystemRegionCache systemRegionCache, @Autowired(required = false) StrixCommonListener strixCommonListener) {
        this.systemRegionService = systemRegionService;
        this.systemManagerService = systemManagerService;
        this.systemRegionCache = systemRegionCache;
        this.strixCommonListener = strixCommonListener;
    }

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区列表")
    public RetResult<SystemRegionListResp> getSystemRegionList(SystemRegionListReq req) {
        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            systemRegionQueryWrapper.like("name", req.getKeyword());
        } else {
            systemRegionQueryWrapper.eq("parent_id", "0");
        }
        if (notSuperManager()) {
            systemRegionQueryWrapper = new QueryWrapper<>();
            systemRegionQueryWrapper.in("id", loginManagerRegionIdList());
            if (StringUtils.hasText(req.getKeyword())) {
                systemRegionQueryWrapper.like("name", req.getKeyword());
            }
        }
        Page<SystemRegion> page = systemRegionService.page(req.getPage(), systemRegionQueryWrapper);
        if (StringUtils.hasText(req.getKeyword())) {
            // 深拷贝
            List<SystemRegion> records = ObjectUtil.clone(page.getRecords());
            records.forEach(r -> {
                String[] parentIds = r.getFullPath().split(",");
                List<SystemRegion> parents = systemRegionService.listByIds(Arrays.asList(parentIds));
                page.getRecords().addAll(parents);
            });
            // 去重
            page.setRecords(new ArrayList<>(page.getRecords().stream().filter(r -> r.getLevel() == 1)
                    .collect(Collectors.toMap(SystemRegion::getId, a -> a, (o1, o2) -> o1)).values()));
        }

        SystemRegionListResp resp = new SystemRegionListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区信息")
    public RetResult<SystemRegionResp> getSystemRegion(@PathVariable String id) {
        Assert.notNull(id, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        return RetBuilder.success(new SystemRegionResp(systemRegion.getId(), systemRegion.getName(), systemRegion.getLevel(), systemRegion.getParentId(), systemRegion.getFullPath(), systemRegion.getFullName(), systemRegion.getRemarks()));
    }

    @GetMapping("{id}/children")
    @PreAuthorize("@ss.hasPermission('system:region')")
    public RetResult<SystemRegionChildrenListResp> getSystemRegionChildren(@PathVariable String id) {
        Assert.notNull(id, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.eq("parent_id", systemRegion.getId());
        List<SystemRegion> childrenList = systemRegionService.list(systemRegionQueryWrapper);
        if (notSuperManager()) {
            Set<SystemRegion> resultList = new HashSet<>();
            loginManagerRegionIdList().forEach(lmr -> childrenList.forEach(c -> {
                if (c.getId().equals(lmr)) {
                    resultList.add(c);
                }
            }));
            return RetBuilder.success(new SystemRegionChildrenListResp(resultList));
        }

        return RetBuilder.success(new SystemRegionChildrenListResp(childrenList));
    }

    @PostMapping("modify/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @StrixLog(operationGroup = "系统地区", operationName = "更改地区信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String id, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");

        if (notSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", loginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        UpdateWrapper<SystemRegion> systemRegionUpdateWrapper = new UpdateWrapper<>();
        systemRegionUpdateWrapper.eq("id", id);

        if ("parentId".equals(singleFieldModifyReq.getField())) {
            systemRegionUpdateWrapper.set("parent_id", id);
            Map<String, String> fullInfo = systemRegionService.getFullInfo(id);
            systemRegionUpdateWrapper.set("full_name", fullInfo.get("name"));
            systemRegionUpdateWrapper.set("full_path", fullInfo.get("path"));
            systemRegionUpdateWrapper.set("level", fullInfo.get("level"));
        } else {
            return RetBuilder.error("参数错误");
        }

        Assert.isTrue(systemRegionService.update(systemRegionUpdateWrapper), "修改失败");
        systemRegionCache.refreshRedisCacheById(id);

        return RetBuilder.success();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:region:add')")
    @StrixLog(operationGroup = "系统地区", operationName = "新增地区", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, "参数错误");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }

        if (notSuperManager()) {
            StrixAssert.in(req.getParentId(), "没有相应的地区权限", loginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        SystemRegion systemRegion = new SystemRegion(
                req.getName(),
                0,
                req.getParentId(),
                null,
                null,
                req.getRemarks()
        );
        systemRegion.setCreateBy(loginManagerId());
        systemRegion.setUpdateBy(loginManagerId());

        UniqueDetectionTool.check(systemRegion);
        Assert.isTrue(systemRegionService.save(systemRegion), "保存失败");

        Map<String, String> fullInfo = systemRegionService.getFullInfo(systemRegion.getId());
        UpdateWrapper<SystemRegion> systemRegionUpdateWrapper = new UpdateWrapper<>();
        systemRegionUpdateWrapper.eq("id", systemRegion.getId());
        systemRegionUpdateWrapper.set("full_name", fullInfo.get("name"));
        systemRegionUpdateWrapper.set("full_path", fullInfo.get("path"));
        systemRegionUpdateWrapper.set("level", fullInfo.get("level"));
        Assert.isTrue(systemRegionService.update(systemRegionUpdateWrapper), "处理信息失败");
        systemRegionCache.refreshRedisCacheById(systemRegion.getId());
        systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());

        return RetBuilder.success();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @StrixLog(operationGroup = "系统地区", operationName = "修改地区", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) SystemRegionUpdateReq req) {
        Assert.hasText(id, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }
        boolean parentChanged = !systemRegion.getParentId().equals(req.getParentId());

        if (notSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", loginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        UpdateWrapper<SystemRegion> updateWrapper = UpdateConditionBuilder.build(systemRegion, req);
        UniqueDetectionTool.check(systemRegion);

        if (parentChanged) {
            systemRegionService.updateRelevantRegion(systemRegion, req.getParentId(), updateWrapper);
        } else {
            Assert.isTrue(systemRegionService.update(updateWrapper), "保存失败");
        }

        return RetBuilder.success();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:remove')")
    @StrixLog(operationGroup = "系统地区", operationName = "删除地区", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        if (notSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", loginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.select("id");
        systemRegionQueryWrapper.likeRight("full_path", systemRegion.getFullPath());
        List<String> removeIdList = systemRegionService.listObjs(systemRegionQueryWrapper, Object::toString);

        // 批量删除
        systemRegionService.removeByIds(removeIdList);
        // 删除管理人员的地区权限关系
        systemManagerService.update(
                new UpdateWrapper<SystemManager>()
                        .set("region_id", null)
                        .in("region_id", removeIdList)
        );

        systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());

        // 循环处理后续工作
        for (String removeId : removeIdList) {
            systemRegionCache.refreshRedisCacheById(removeId);

            if (strixCommonListener != null) {
                strixCommonListener.deleteSystemRegionNotify(removeId);
            }
        }

        return RetBuilder.success();
    }

    @GetMapping("cascader")
    public RetResult<CommonCascaderDataResp> getCascaderData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (notSuperManager()) {
            List<SystemRegion> resultList = new ArrayList<>();
            Set<String> returnIdList = new HashSet<>();
            systemRegionList.forEach(r -> loginManagerRegionIdList().forEach(id -> {
                if (r.getFullPath().contains("," + id + ",")) {
                    returnIdList.addAll(Arrays.asList(r.getFullPath().split(",")));
                }
            }));
            returnIdList.forEach(r -> systemRegionList.forEach(region -> {
                if (region.getId().equals(r)) {
                    resultList.add(region);
                }
            }));
            return RetBuilder.success(new CommonCascaderDataResp(resultList));
        }
        return RetBuilder.success(new CommonCascaderDataResp(systemRegionList));
    }

    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getTreeData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (notSuperManager()) {
            List<SystemRegion> resultList = new ArrayList<>();
            Set<String> returnIdList = new HashSet<>();
            systemRegionList.forEach(r -> loginManagerRegionIdList().forEach(id -> {
                if (r.getFullPath().contains("," + id + ",")) {
                    returnIdList.addAll(Arrays.asList(r.getFullPath().split(",")));
                }
            }));
            returnIdList.forEach(r -> systemRegionList.forEach(region -> {
                if (region.getId().equals(r)) {
                    resultList.add(region);
                }
            }));
            return RetBuilder.success(new CommonTreeDataResp(resultList));
        }
        return RetBuilder.success(new CommonTreeDataResp(systemRegionList));
    }

}
