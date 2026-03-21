package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.oss.OssConfigListReq;
import cn.projectan.strix.model.request.system.module.oss.OssConfigUpdateReq;
import cn.projectan.strix.model.request.system.module.oss.OssFileListReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.system.module.oss.*;
import cn.projectan.strix.service.system.OssBucketService;
import cn.projectan.strix.service.system.OssConfigService;
import cn.projectan.strix.service.system.OssFileGroupService;
import cn.projectan.strix.service.system.OssFileService;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储配置管理
 *
 * @author ProjectAn
 * @since 2023/5/23 11:46
 */
@Slf4j
@RestController
@RequestMapping("system/oss")
@ConditionalOnBean(StrixOssStore.class)
@RequiredArgsConstructor
public class OssController extends BaseSystemController {

    private final OssConfigService ossConfigService;
    private final OssBucketService ossBucketService;
    private final OssFileService ossFileService;
    private final OssFileGroupService ossFileGroupService;

    /**
     * 查询存储配置列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config')")
    @StrixLog(operationGroup = "系统存储", operationName = "查询存储配置列表")
    public RetResult<OssConfigListResp> getList(OssConfigListReq req) {
        Page<OssConfig> page = ossConfigService.listPage(req);
        return RetBuilder.success(new OssConfigListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询存储配置信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config')")
    @StrixLog(operationGroup = "系统存储", operationName = "查询存储配置信息")
    public RetResult<OssConfigResp> getInfo(@PathVariable String id) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "配置不存在");

        List<OssBucket> buckets = ossBucketService.listByConfigKey(ossConfig.getKey());
        List<OssBucketListResp.OssBucketItem> bucketItems = new OssBucketListResp(buckets, (long) buckets.size()).getBuckets();

        List<OssFileGroup> fileGroups = ossFileGroupService.listByConfigKey(ossConfig.getKey());
        List<OssFileGroupListResp.OssFileGroupItem> fileGroupItems = new OssFileGroupListResp(fileGroups, (long) fileGroups.size()).getFileGroups();

        return RetBuilder.success(
                new OssConfigResp(
                        ossConfig.getId(),
                        ossConfig.getKey(),
                        ossConfig.getName(),
                        ossConfig.getPlatform(),
                        ossConfig.getRegion(),
                        ossConfig.getPublicEndpoint(),
                        ossConfig.getPrivateEndpoint(),
                        ossConfig.getAccessKey(),
                        ossConfig.getRemark(),
                        ossConfig.getCreatedTime(),
                        bucketItems,
                        fileGroupItems
                )
        );
    }

    /**
     * 新增存储配置
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:add')")
    @StrixLog(operationGroup = "系统存储", operationName = "新增存储配置", operationType = SystemLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) OssConfigUpdateReq req) {
        OssConfig ossConfig = new OssConfig(
                req.getKey(),
                req.getName(),
                req.getPlatform(),
                req.getRegion(),
                req.getPublicEndpoint(),
                req.getPrivateEndpoint(),
                req.getAccessKey(),
                req.getAccessSecret(),
                req.getRemark()
        );

        UniqueChecker.check(ossConfig);

        Assert.isTrue(ossConfigService.save(ossConfig), "保存失败");

        // 重新加载配置
        ossConfigService.refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 修改存储配置
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:update')")
    @StrixLog(operationGroup = "系统存储", operationName = "修改存储配置", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) OssConfigUpdateReq req) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String originKey = ossConfig.getKey();

        LambdaUpdateWrapper<OssConfig> updateWrapper = UpdateBuilder.build(ossConfig, req);
        UniqueChecker.check(ossConfig);
        Assert.isTrue(ossConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        ossConfigService.removeInstance(originKey);
        ossConfigService.refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 删除存储配置
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:remove')")
    @StrixLog(operationGroup = "系统存储", operationName = "删除存储配置", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String key = ossConfig.getKey();

        ossConfigService.removeById(id);

        // 删除Bucket配置, 但不删除文件组和文件
        ossBucketService.deleteByConfigKey(key);

        // 卸载配置
        ossConfigService.removeInstance(key);

        return RetBuilder.success();
    }

    /**
     * 查询存储配置下拉列表
     */
    @GetMapping("config/select")
    public RetResult<CommonSelectDataResp> getOssConfigSelectList() {
        return RetBuilder.success(ossConfigService.getSelectData());
    }

    /**
     * 查询存储文件列表
     */
    @GetMapping("file")
    @PreAuthorize("@ss.hasPermission('system:module:oss:file')")
    @StrixLog(operationGroup = "系统存储", operationName = "查询存储文件列表")
    public RetResult<OssFileListResp> getOssFileList(OssFileListReq req) {
        Page<OssFile> page = ossFileService.listPage(req);

        return RetBuilder.success(new OssFileListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 删除存储文件
     */
    @PostMapping("file/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:file:remove')")
    @StrixLog(operationGroup = "系统存储", operationName = "删除存储文件", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> removeFile(@PathVariable String id) {
        ossFileService.delete(id);
        return RetBuilder.success();
    }

}
