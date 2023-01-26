package cn.projectan.strix.controller.system;

import cn.hutool.core.util.ObjectUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.listener.StrixCommonListener;
import cn.projectan.strix.core.ramcache.SystemRegionCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.NeedSystemPermission;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.systemregion.SystemRegionListQueryReq;
import cn.projectan.strix.model.request.system.systemregion.SystemRegionUpdateReq;
import cn.projectan.strix.model.response.common.CommonCascaderDataResp;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.model.response.system.systemregion.SystemRegionChildrenListResp;
import cn.projectan.strix.model.response.system.systemregion.SystemRegionListQueryResp;
import cn.projectan.strix.model.response.system.systemregion.SystemRegionQueryByIdResp;
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

    @Autowired
    private SystemRegionService systemRegionService;
    @Autowired
    private SystemManagerService systemManagerService;
    @Autowired
    private SystemRegionCache systemRegionCache;

    @Autowired(required = false)
    private StrixCommonListener strixCommonListener;

    @GetMapping("")
    @NeedSystemPermission("System_Region")
    public RetResult<SystemRegionListQueryResp> getSystemRegionList(SystemRegionListQueryReq req) {
        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            systemRegionQueryWrapper.like("name", req.getKeyword());
        } else {
            systemRegionQueryWrapper.eq("parent_id", "0");
        }
        if (!isLoggedInSuperManager()) {
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

        SystemRegionListQueryResp resp = new SystemRegionListQueryResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }

    @GetMapping("{id}")
    @NeedSystemPermission("System_Region")
    public RetResult<SystemRegionQueryByIdResp> getSystemRegion(@PathVariable String id) {
        Assert.notNull(id, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        return RetMarker.makeSuccessRsp(new SystemRegionQueryByIdResp(systemRegion.getId(), systemRegion.getName(), systemRegion.getLevel(), systemRegion.getParentId(), systemRegion.getFullPath(), systemRegion.getFullName(), systemRegion.getRemarks()));
    }

    @GetMapping("{id}/children")
    @NeedSystemPermission("System_Region")
    public RetResult<SystemRegionChildrenListResp> getSystemRegionChildren(@PathVariable String id) {
        Assert.notNull(id, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.eq("parent_id", systemRegion.getId());
        List<SystemRegion> childrenList = systemRegionService.list(systemRegionQueryWrapper);
        if (!isLoggedInSuperManager()) {
            Set<SystemRegion> resultList = new HashSet<>();
            getLoginManagerRegionIdList().forEach(lmr -> {
                childrenList.forEach(c -> {
                    if (c.getId().equals(lmr)) {
                        resultList.add(c);
                    }
                });
            });
            return RetMarker.makeSuccessRsp(new SystemRegionChildrenListResp(resultList));
        }

        return RetMarker.makeSuccessRsp(new SystemRegionChildrenListResp(childrenList));
    }

    @PostMapping("modify/{id}")
    @NeedSystemPermission(value = "System_Region", isEdit = true)
    public RetResult<Object> modifyField(@PathVariable String id, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");

        if (!isLoggedInSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        UpdateWrapper<SystemRegion> systemRegionUpdateWrapper = new UpdateWrapper<>();
        systemRegionUpdateWrapper.eq("id", id);

        switch (singleFieldModifyReq.getField()) {
            case "parentId":
                systemRegionUpdateWrapper.set("parent_id", id);
                Map<String, String> fullInfo = systemRegionService.getFullInfo(id);
                systemRegionUpdateWrapper.set("full_name", fullInfo.get("name"));
                systemRegionUpdateWrapper.set("full_path", fullInfo.get("path"));
                systemRegionUpdateWrapper.set("level", fullInfo.get("level"));
                break;
            default:
                return RetMarker.makeErrRsp("参数错误");
        }

        Assert.isTrue(systemRegionService.update(systemRegionUpdateWrapper), "修改失败");
        systemRegionCache.refreshRedisCacheById(id);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update")
    @NeedSystemPermission(value = "System_Region", isEdit = true)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, "参数错误");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }

        if (!isLoggedInSuperManager()) {
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
    @NeedSystemPermission(value = "System_Region", isEdit = true)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) SystemRegionUpdateReq req) {
        Assert.hasText(id, "参数错误");
        Assert.notNull(req, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        if (!StringUtils.hasText(req.getParentId())) {
            req.setParentId("0");
        }
        boolean parentChanged = !systemRegion.getParentId().equals(req.getParentId());

        if (!isLoggedInSuperManager()) {
            StrixAssert.in(id, "没有相应的地区权限", getLoginManagerRegionIdListExcludeCurrent().toArray(new String[0]));
        }

        UpdateWrapper<SystemRegion> updateWrapper = UpdateConditionBuilder.build(systemRegion, req, getLoginManagerId());
        UniqueDetectionTool.check(systemRegion);

        if (parentChanged) {
            systemRegionService.updateRelevantRegion(systemRegion, req.getParentId(), updateWrapper);
        } else {
            Assert.isTrue(systemRegionService.update(updateWrapper), "保存失败");
        }

//        Assert.isTrue(systemRegionService.update(updateWrapper), "保存失败");
//
//        systemRegionCache.refreshRedisCacheById(systemRegion.getId());
//        systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());
//        List<String> childrenIdList = systemRegionService.getChildrenIdList(systemRegion.getId());
//        childrenIdList.forEach(cid -> {
//            Map<String, String> fullInfo = systemRegionService.getFullInfo(cid);
//            UpdateWrapper<SystemRegion> systemRegionUpdateWrapper = new UpdateWrapper<>();
//            systemRegionUpdateWrapper.eq("id", cid);
//            systemRegionUpdateWrapper.set("full_name", fullInfo.get("name"));
//            systemRegionUpdateWrapper.set("full_path", fullInfo.get("path"));
//            systemRegionUpdateWrapper.set("level", fullInfo.get("level"));
//            Assert.isTrue(systemRegionService.update(systemRegionUpdateWrapper), "处理信息失败");
//            systemRegionCache.refreshRedisCacheById(id);
//        });

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @NeedSystemPermission(value = "System_Region", isEdit = true)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        if (!isLoggedInSuperManager()) {
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
    @NeedSystemPermission
    public RetResult<CommonCascaderDataResp> getCascaderData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (!isLoggedInSuperManager()) {
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
    @NeedSystemPermission
    public RetResult<CommonTreeDataResp> getTreeData() {
        List<SystemRegion> systemRegionList = systemRegionService.list();
        if (!isLoggedInSuperManager()) {
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
