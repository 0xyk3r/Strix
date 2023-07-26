package cn.projectan.strix.controller.system;

import cn.hutool.core.util.ObjectUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemRegionCache;
import cn.projectan.strix.core.listener.StrixCommonListener;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.constant.SysLogOperType;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.region.SystemRegionListReq;
import cn.projectan.strix.model.request.system.region.SystemRegionUpdateReq;
import cn.projectan.strix.model.response.common.CommonCascaderDataResp;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.region.SystemRegionChildrenListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionListResp;
import cn.projectan.strix.model.response.system.region.SystemRegionResp;
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
 * @author 安炯奕
 * @date 2021/9/29 17:45
 */
@Slf4j
@RestController
@RequestMapping("system/region")
public class SystemRegionController extends BaseSystemController {

    private final SystemRegionService systemRegionService;
    private final SystemRegionCache systemRegionCache;
    private final StrixCommonListener strixCommonListener;

    @Autowired
    public SystemRegionController(SystemRegionService systemRegionService, SystemRegionCache systemRegionCache, @Autowired(required = false) StrixCommonListener strixCommonListener) {
        this.systemRegionService = systemRegionService;
        this.systemRegionCache = systemRegionCache;
        this.strixCommonListener = strixCommonListener;
    }

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @SysLog(operationGroup = "系统地区", operationName = "查询地区列表")
    public RetResult<SystemRegionListResp> getSystemRegionList(SystemRegionListReq req) {
        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            systemRegionQueryWrapper.like("name", req.getKeyword());
        } else {
            systemRegionQueryWrapper.eq("parent_id", "0");
        }
        if (!isSuperManager()) {
            systemRegionQueryWrapper = new QueryWrapper<>();
            systemRegionQueryWrapper.in("id", getLoginManagerRegionIdList());
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

        return RetMarker.makeSuccessRsp(resp);
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @SysLog(operationGroup = "系统地区", operationName = "查询地区信息")
    public RetResult<SystemRegionResp> getSystemRegion(@PathVariable String id) {
        Assert.notNull(id, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        return RetMarker.makeSuccessRsp(new SystemRegionResp(systemRegion.getId(), systemRegion.getName(), systemRegion.getLevel(), systemRegion.getParentId(), systemRegion.getFullPath(), systemRegion.getFullName(), systemRegion.getRemarks()));
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
        if (!isSuperManager()) {
            Set<SystemRegion> resultList = new HashSet<>();
            getLoginManagerRegionIdList().forEach(lmr -> childrenList.forEach(c -> {
                if (c.getId().equals(lmr)) {
                    resultList.add(c);
                }
            }));
            return RetMarker.makeSuccessRsp(new SystemRegionChildrenListResp(resultList));
        }

        return RetMarker.makeSuccessRsp(new SystemRegionChildrenListResp(childrenList));
    }

    @PostMapping("modify/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @SysLog(operationGroup = "系统地区", operationName = "更改地区信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String id, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");

        if (!isSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
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
            return RetMarker.makeErrRsp("参数错误");
        }

        Assert.isTrue(systemRegionService.update(systemRegionUpdateWrapper), "修改失败");
        systemRegionCache.refreshRedisCacheById(id);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:region:add')")
    @SysLog(operationGroup = "系统地区", operationName = "新增地区", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, "参数错误");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }

        if (!isSuperManager()) {
            StrixAssert.in(req.getParentId(), "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        SystemRegion systemRegion = new SystemRegion(
                req.getName(),
                0,
                req.getParentId(),
                null,
                null,
                req.getRemarks()
        );
        systemRegion.setCreateBy(getLoginManagerId());
        systemRegion.setUpdateBy(getLoginManagerId());

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

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @SysLog(operationGroup = "系统地区", operationName = "修改地区", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) SystemRegionUpdateReq req) {
        Assert.hasText(id, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }
        boolean parentChanged = !systemRegion.getParentId().equals(req.getParentId());

        if (!isSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        UpdateWrapper<SystemRegion> updateWrapper = UpdateConditionBuilder.build(systemRegion, req, getLoginManagerId());
        UniqueDetectionTool.check(systemRegion);

        if (parentChanged) {
            systemRegionService.updateRelevantRegion(systemRegion, req.getParentId(), updateWrapper);
        } else {
            Assert.isTrue(systemRegionService.update(updateWrapper), "保存失败");
        }

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:remove')")
    @SysLog(operationGroup = "系统地区", operationName = "删除地区", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        if (!isSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.select("id");
        systemRegionQueryWrapper.likeRight("full_path", systemRegion.getFullPath());
        List<String> removeIdList = systemRegionService.listObjs(systemRegionQueryWrapper, Object::toString);

        // 批量删除
        systemRegionService.removeByIds(removeIdList);

        systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());


        // 循环处理后续工作
        for (String removeId : removeIdList) {
            systemRegionCache.refreshRedisCacheById(removeId);

            // TODO 删除管理人员的地区权限关系

            if (strixCommonListener != null) {
                strixCommonListener.deleteSystemRegionNotify(removeId);
            }
        }

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("cascader")
    public RetResult<CommonCascaderDataResp> getCascaderData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (!isSuperManager()) {
            List<SystemRegion> resultList = new ArrayList<>();
            Set<String> returnIdList = new HashSet<>();
            systemRegionList.forEach(r -> getLoginManagerRegionIdList().forEach(id -> {
                if (r.getFullPath().contains("," + id + ",")) {
                    returnIdList.addAll(Arrays.asList(r.getFullPath().split(",")));
                }
            }));
            returnIdList.forEach(r -> systemRegionList.forEach(region -> {
                if (region.getId().equals(r)) {
                    resultList.add(region);
                }
            }));
            return RetMarker.makeSuccessRsp(new CommonCascaderDataResp(resultList));
        }
        return RetMarker.makeSuccessRsp(new CommonCascaderDataResp(systemRegionList));
    }

    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getTreeData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (!isSuperManager()) {
            List<SystemRegion> resultList = new ArrayList<>();
            Set<String> returnIdList = new HashSet<>();
            systemRegionList.forEach(r -> getLoginManagerRegionIdList().forEach(id -> {
                if (r.getFullPath().contains("," + id + ",")) {
                    returnIdList.addAll(Arrays.asList(r.getFullPath().split(",")));
                }
            }));
            returnIdList.forEach(r -> systemRegionList.forEach(region -> {
                if (region.getId().equals(r)) {
                    resultList.add(region);
                }
            }));
            return RetMarker.makeSuccessRsp(new CommonTreeDataResp(resultList));
        }
        return RetMarker.makeSuccessRsp(new CommonTreeDataResp(systemRegionList));
    }

}
