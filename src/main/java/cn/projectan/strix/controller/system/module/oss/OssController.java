package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.module.oss.OssConfigListReq;
import cn.projectan.strix.model.request.module.oss.OssConfigUpdateReq;
import cn.projectan.strix.model.request.module.oss.OssFileListReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.module.oss.*;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.service.OssFileGroupService;
import cn.projectan.strix.service.OssFileService;
import cn.projectan.strix.task.StrixOssTask;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储配置管理
 *
 * @author ProjectAn
 * @date 2023/5/23 11:46
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
        Page<OssConfig> page = ossConfigService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssConfig::getKey, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(OssConfig::getName, req.getKeyword()))
                .page(req.getPage());
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

        List<OssBucket> buckets = ossBucketService.lambdaQuery()
                .eq(OssBucket::getConfigKey, ossConfig.getKey())
                .list();
        List<OssBucketListResp.OssBucketItem> bucketItems = new OssBucketListResp(buckets, (long) buckets.size()).getBuckets();

        List<OssFileGroup> fileGroups = ossFileGroupService.lambdaQuery()
                .eq(OssFileGroup::getConfigKey, ossConfig.getKey())
                .list();
        List<OssFileGroupListResp.OssFileGroupItem> fileGroupItems = new OssFileGroupListResp(fileGroups, (long) fileGroups.size()).getFileGroups();

        return RetBuilder.success(
                new OssConfigResp(
                        ossConfig.getId(),
                        ossConfig.getKey(),
                        ossConfig.getName(),
                        ossConfig.getPlatform(),
                        ossConfig.getPublicEndpoint(),
                        ossConfig.getPrivateEndpoint(),
                        ossConfig.getAccessKey(),
                        ossConfig.getRemark(),
                        ossConfig.getCreateTime(),
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
    @StrixLog(operationGroup = "系统存储", operationName = "新增存储配置", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) OssConfigUpdateReq req) {
        OssConfig ossConfig = new OssConfig(
                req.getKey(),
                req.getName(),
                req.getPlatform(),
                req.getPublicEndpoint(),
                req.getPrivateEndpoint(),
                req.getAccessKey(),
                req.getAccessSecret(),
                req.getRemark()
        );

        UniqueDetectionTool.check(ossConfig);

        Assert.isTrue(ossConfigService.save(ossConfig), "保存失败");

        // 重新加载配置
        SpringUtil.getBean(StrixOssTask.class).refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 修改存储配置
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:update')")
    @StrixLog(operationGroup = "系统存储", operationName = "修改存储配置", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) OssConfigUpdateReq req) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String originKey = ossConfig.getKey();

        LambdaUpdateWrapper<OssConfig> updateWrapper = UpdateConditionBuilder.build(ossConfig, req);
        UniqueDetectionTool.check(ossConfig);
        Assert.isTrue(ossConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        SpringUtil.getBean(StrixOssStore.class).getInstance(originKey).close();
        SpringUtil.getBean(StrixOssTask.class).refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 删除存储配置
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:remove')")
    @StrixLog(operationGroup = "系统存储", operationName = "删除存储配置", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String key = ossConfig.getKey();

        ossConfigService.removeById(id);

        // 删除Bucket配置, 但不删除文件组和文件
        ossBucketService.lambdaUpdate()
                .eq(OssBucket::getConfigKey, key)
                .remove();

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
        Page<OssFile> page = ossFileService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssFile::getPath, req.getKeyword())
                .eq(StringUtils.hasText(req.getConfigKey()), OssFile::getConfigKey, req.getConfigKey())
                .eq(StringUtils.hasText(req.getGroupKey()), OssFile::getGroupKey, req.getGroupKey())
                .page(req.getPage());

        return RetBuilder.success(new OssFileListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 删除存储文件
     */
    @PostMapping("file/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:file:remove')")
    @StrixLog(operationGroup = "系统存储", operationName = "删除存储文件", operationType = SysLogOperType.DELETE)
    public RetResult<Object> removeFile(@PathVariable String id) {
        ossFileService.delete(id);
        return RetBuilder.success();
    }

}
