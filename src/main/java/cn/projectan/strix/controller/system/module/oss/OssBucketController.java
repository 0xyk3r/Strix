package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.oss.OssBucketListReq;
import cn.projectan.strix.model.request.system.module.oss.OssBucketUpdateReq;
import cn.projectan.strix.model.response.system.module.oss.OssBucketListResp;
import cn.projectan.strix.service.system.OssBucketService;
import cn.projectan.strix.task.system.StrixOssTask;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 存储空间管理
 *
 * @author ProjectAn
 * @since 2023/5/27 22:43
 */
@Slf4j
@RestController
@RequestMapping("system/oss/bucket")
@ConditionalOnBean(StrixOssStore.class)
@RequiredArgsConstructor
@Tag(name = "系统模块 - OSS Bucket 管理")
public class OssBucketController extends BaseSystemController {

    private final OssBucketService ossBucketService;
    private final StrixOssTask strixOssTask;

    /**
     * 查询存储空间列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "查询存储空间列表")
    @Operation(summary = "Bucket 列表")
    public RetResult<OssBucketListResp> getOssBucketList(OssBucketListReq req) {
        Page<OssBucket> page = ossBucketService.listPage(req);

        return RetBuilder.success(new OssBucketListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 新增存储空间
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:add')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "新增存储空间", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新增 Bucket")
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) OssBucketUpdateReq req) {
        OssBucket ossBucket = new OssBucket(
                req.getConfigKey(),
                req.getName(),
                null
        );

        UniqueChecker.check(ossBucket);

        ossBucketService.createBucket(ossBucket.getConfigKey(), ossBucket.getName());
        strixOssTask.refreshBucketList();

        return RetBuilder.success();
    }

    /**
     * 修改存储空间
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:update')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "修改存储空间", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "编辑 Bucket")
    public RetResult<Object> update(@Parameter(description = "Bucket ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) OssBucketUpdateReq req) {
        OssBucket ossBucket = ossBucketService.getById(id);
        Assert.notNull(ossBucket, "原记录不存在");

        LambdaUpdateWrapper<OssBucket> updateWrapper = UpdateBuilder.build(ossBucket, req);
        UniqueChecker.check(ossBucket);
        Assert.isTrue(ossBucketService.update(updateWrapper), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 删除存储空间
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:remove')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "删除存储空间", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除 Bucket")
    public RetResult<Object> remove(@Parameter(description = "Bucket ID") @PathVariable String id) {
        ossBucketService.removeById(id);
        return RetBuilder.success();
    }

}
