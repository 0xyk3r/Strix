package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.config.StrixOssConfig;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.constant.StrixOssPlatform;
import cn.projectan.strix.model.constant.SysLogOperType;
import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.db.OssFileGroup;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/23 11:46
 */
@Slf4j
@RestController
@RequestMapping("system/oss")
@RequiredArgsConstructor
public class OssController extends BaseSystemController {

    private final OssConfigService ossConfigService;
    private final OssBucketService ossBucketService;
    private final OssFileService ossFileService;
    private final OssFileGroupService ossFileGroupService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config')")
    @SysLog(operationGroup = "系统存储", operationName = "查询存储配置列表")
    public RetResult<OssConfigListResp> getList(OssConfigListReq req) {
        QueryWrapper<OssConfig> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("`key`", req.getKeyword())
                    .or(q -> q.like("`name`", req.getKeyword()));
        }

        Page<OssConfig> page = ossConfigService.page(req.getPage(), queryWrapper);
        return RetMarker.makeSuccessRsp(new OssConfigListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config')")
    @SysLog(operationGroup = "系统存储", operationName = "查询存储配置信息")
    public RetResult<OssConfigResp> getInfo(@PathVariable String id) {
        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "配置不存在");

        List<OssBucket> buckets = ossBucketService.lambdaQuery().eq(OssBucket::getConfigKey, ossConfig.getKey()).list();
        List<OssBucketListResp.OssBucketItem> bucketItems = new OssBucketListResp(buckets, (long) buckets.size()).getBuckets();

        List<OssFileGroup> fileGroups = ossFileGroupService.lambdaQuery().eq(OssFileGroup::getConfigKey, ossConfig.getKey()).list();
        List<OssFileGroupListResp.OssFileGroupItem> fileGroupItems = new OssFileGroupListResp(fileGroups, (long) fileGroups.size()).getFileGroups();

        return RetMarker.makeSuccessRsp(
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

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:add')")
    @SysLog(operationGroup = "系统存储", operationName = "新增存储配置", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) OssConfigUpdateReq req) {
        Assert.isTrue(StrixOssPlatform.valid(req.getPlatform()), "请选择正确的服务平台");

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
        ossConfig.setCreateBy(getLoginManagerId());
        ossConfig.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(ossConfig);

        Assert.isTrue(ossConfigService.save(ossConfig), "保存失败");

        // 重新加载配置
        SpringUtil.getBean(StrixOssTask.class).refreshConfig();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:update')")
    @SysLog(operationGroup = "系统存储", operationName = "修改存储配置", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) OssConfigUpdateReq req) {
        Assert.isTrue(StrixOssPlatform.valid(req.getPlatform()), "请选择正确的服务平台");

        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String originKey = ossConfig.getKey();

        UpdateWrapper<OssConfig> updateWrapper = UpdateConditionBuilder.build(ossConfig, req, getLoginManagerId());
        UniqueDetectionTool.check(ossConfig);
        Assert.isTrue(ossConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        SpringUtil.getBean(StrixOssConfig.class).getInstance(originKey).close();
        SpringUtil.getBean(StrixOssTask.class).refreshConfig();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:config:remove')")
    @SysLog(operationGroup = "系统存储", operationName = "删除存储配置", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        OssConfig ossConfig = ossConfigService.getById(id);
        Assert.notNull(ossConfig, "原记录不存在");
        String key = ossConfig.getKey();

        ossConfigService.removeById(id);

        // 删除 Bucket 配置   但不删除 文件组 和 文件
        ossBucketService.remove(new LambdaQueryWrapper<>(OssBucket.class).eq(OssBucket::getConfigKey, key));

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("config/select")
    public RetResult<CommonSelectDataResp> getOssConfigSelectList() {
        return RetMarker.makeSuccessRsp(ossConfigService.getSelectData());
    }

    @GetMapping("file")
    @PreAuthorize("@ss.hasPermission('system:module:oss:file')")
    @SysLog(operationGroup = "系统存储", operationName = "查询存储文件列表")
    public RetResult<OssFileListResp> getOssFileList(OssFileListReq req) {
        QueryWrapper<OssFile> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }
        if (StringUtils.hasText(req.getGroupKey())) {
            queryWrapper.eq("group_key", req.getGroupKey());
        }

        Page<OssFile> page = ossFileService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new OssFileListResp(page.getRecords(), page.getTotal()));
    }

    @PostMapping("file/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:file:remove')")
    @SysLog(operationGroup = "系统存储", operationName = "删除存储文件", operationType = SysLogOperType.DELETE)
    public RetResult<Object> removeFile(@PathVariable String id) {
        ossFileService.delete(id);
        return RetMarker.makeSuccessRsp();
    }

}
