package cn.projectan.strix.controller.system;

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
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统地区
 *
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

    /**
     * 获取地区列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区列表")
    public RetResult<SystemRegionListResp> getSystemRegionList(SystemRegionListReq req) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        int maxRegionLevel = 0;
        if (notSuperManager()) {
            // 需要获取当前可用的最大地区权限等级
            maxRegionLevel = systemRegionService.lambdaQuery()
                    .select(SystemRegion::getLevel)
                    .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemRegion::getId, loginManagerRegionPermissions)
                    .orderByAsc(SystemRegion::getLevel)
                    .last("limit 1")
                    .oneOpt()
                    .map(SystemRegion::getLevel)
                    .orElse(0);
        }

        Page<SystemRegion> page = systemRegionService.lambdaQuery()
                .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemRegion::getId, loginManagerRegionPermissions)
                .like(StringUtils.hasText(req.getKeyword()), SystemRegion::getName, req.getKeyword())
                .eq(notSuperManager() && !StringUtils.hasText(req.getKeyword()), SystemRegion::getLevel, maxRegionLevel)
                .eq(isSuperManager() && !StringUtils.hasText(req.getKeyword()), SystemRegion::getParentId, "0")
                .page(req.getPage());


        // 这里是搜索子节点时, 完整查询从根节点开始的所有地区, 暂时注释掉.
//        if (StringUtils.hasText(req.getKeyword())) {
//            // 深拷贝
//            List<SystemRegion> records = ObjectUtil.clone(page.getRecords());
//            records.forEach(r -> {
//                String[] parentIds = r.getFullPath().split(",");
//                List<SystemRegion> parents = systemRegionService.listByIds(Arrays.asList(parentIds));
//                page.getRecords().addAll(parents);
//            });
//            // 去重
//            page.setRecords(new ArrayList<>(page.getRecords().stream().filter(r -> r.getLevel() == 1)
//                    .collect(Collectors.toMap(SystemRegion::getId, a -> a, (o1, o2) -> o1)).values()));
//        }

        SystemRegionListResp resp = new SystemRegionListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 获取地区信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:region')")
    @StrixLog(operationGroup = "系统地区", operationName = "查询地区信息")
    public RetResult<SystemRegionResp> getSystemRegion(@PathVariable String id) {
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        checkLoginManagerRegionPermission(id);

        return RetBuilder.success(new SystemRegionResp(systemRegion.getId(), systemRegion.getName(), systemRegion.getLevel(), systemRegion.getParentId(), systemRegion.getFullPath(), systemRegion.getFullName(), systemRegion.getRemarks()));
    }

    /**
     * 获取地区子节点
     */
    @GetMapping("{id}/children")
    @PreAuthorize("@ss.hasPermission('system:region')")
    public RetResult<SystemRegionChildrenListResp> getSystemRegionChildren(@PathVariable String id) {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        checkLoginManagerRegionPermission(id);

        List<SystemRegion> childrenList = systemRegionService.lambdaQuery()
                .eq(SystemRegion::getParentId, systemRegion.getId())
                .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemRegion::getId, loginManagerRegionPermissions)
                .list();

        return RetBuilder.success(new SystemRegionChildrenListResp(childrenList));
    }

    /**
     * 更改地区信息
     */
    @PostMapping("modify/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @StrixLog(operationGroup = "系统地区", operationName = "更改地区信息", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> modifyField(@PathVariable String id, @RequestBody SingleFieldModifyReq singleFieldModifyReq) {
        Assert.hasText(singleFieldModifyReq.getField(), "参数错误");
        Assert.isTrue("parentId".equals(singleFieldModifyReq.getField()), "参数错误");

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        checkLoginManagerRegionPermission(id);

        Map<String, String> fullInfo = systemRegionService.getFullInfo(id);
        Assert.isTrue(
                systemRegionService.lambdaUpdate()
                        .eq(SystemRegion::getId, id)
                        .set(SystemRegion::getParentId, id)
                        .set(SystemRegion::getFullName, fullInfo.get("name"))
                        .set(SystemRegion::getFullPath, fullInfo.get("path"))
                        .set(SystemRegion::getLevel, fullInfo.get("level"))
                        .update(),
                "修改失败"
        );

        systemRegionCache.refreshRedisCacheById(id);

        return RetBuilder.success();
    }

    /**
     * 新增地区
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:region:add')")
    @StrixLog(operationGroup = "系统地区", operationName = "新增地区", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, "参数错误");
        if (!StringUtils.hasText(req.getParentId())) {
            if (notSuperManager()) {
                req.setParentId(loginManagerRegionId());
            } else {
                req.setParentId("0");
            }
        }
        checkLoginManagerRegionPermission(req.getParentId());

        SystemRegion systemRegion = new SystemRegion(
                req.getName(),
                0,
                req.getParentId(),
                null,
                null,
                req.getRemarks()
        );

        UniqueDetectionTool.check(systemRegion);
        Assert.isTrue(systemRegionService.save(systemRegion), "保存失败");

        Map<String, String> fullInfo = systemRegionService.getFullInfo(systemRegion.getId());
        Assert.isTrue(
                systemRegionService.lambdaUpdate()
                        .eq(SystemRegion::getId, systemRegion.getId())
                        .set(SystemRegion::getFullName, fullInfo.get("name"))
                        .set(SystemRegion::getFullPath, fullInfo.get("path"))
                        .set(SystemRegion::getLevel, fullInfo.get("level"))
                        .update()
                , "处理信息失败");

        systemRegionCache.refreshRedisCacheById(systemRegion.getId());
        systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());

        return RetBuilder.success();
    }

    /**
     * 修改地区
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:update')")
    @StrixLog(operationGroup = "系统地区", operationName = "修改地区", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) SystemRegionUpdateReq req) {
        Assert.notNull(req, "参数错误");
        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");
        if (!StringUtils.hasText(req.getParentId())) {
            if (notSuperManager()) {
                req.setParentId(loginManagerRegionId());
            } else {
                req.setParentId("0");
            }
        }
        checkLoginManagerRegionPermission(req.getParentId());

        boolean parentChanged = !systemRegion.getParentId().equals(req.getParentId());

        LambdaUpdateWrapper<SystemRegion> updateWrapper = UpdateConditionBuilder.build(systemRegion, req);
        UniqueDetectionTool.check(systemRegion);

        if (parentChanged) {
            systemRegionService.updateRelevantRegion(systemRegion, req.getParentId(), updateWrapper);
        } else {
            Assert.isTrue(systemRegionService.update(updateWrapper), "保存失败");
        }

        return RetBuilder.success();
    }

    /**
     * 删除地区
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:region:remove')")
    @StrixLog(operationGroup = "系统地区", operationName = "删除地区", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        checkLoginManagerRegionPermission(id);

        SystemRegion systemRegion = systemRegionService.getById(id);
        Assert.notNull(systemRegion, "系统地区信息不存在");

        List<String> removeIdList = systemRegionService.lambdaQuery()
                .select(SystemRegion::getId)
                .likeRight(SystemRegion::getFullPath, systemRegion.getFullPath())
                .list()
                .stream()
                .map(SystemRegion::getId)
                .collect(Collectors.toList());

        // 批量删除
        systemRegionService.removeByIds(removeIdList);
        // 删除管理人员的地区权限关系
        systemManagerService.lambdaUpdate()
                .in(SystemManager::getRegionId, removeIdList)
                .set(SystemManager::getRegionId, null)
                .update();

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

    /**
     * 获取地区级联数据
     */
    @GetMapping("cascader")
    public RetResult<CommonCascaderDataResp> getCascaderData() {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        List<SystemRegion> systemRegionList = systemRegionService.lambdaQuery()
                .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemRegion::getId, loginManagerRegionPermissions)
                .list();
        return RetBuilder.success(new CommonCascaderDataResp(systemRegionList));
    }

    /**
     * 获取地区树形数据
     */
    @GetMapping("tree")
    public RetResult<CommonTreeDataResp> getTreeData() {
        List<String> loginManagerRegionPermissions = loginManagerRegionPermissions();
        List<SystemRegion> systemRegionList = systemRegionService.lambdaQuery()
                .in(!CollectionUtils.isEmpty(loginManagerRegionPermissions), SystemRegion::getId, loginManagerRegionPermissions)
                .list();
        return RetBuilder.success(new CommonTreeDataResp(systemRegionList));
    }

}
