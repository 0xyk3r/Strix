package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssConfig;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.module.oss.OssBucketListReq;
import cn.projectan.strix.model.request.module.oss.OssBucketUpdateReq;
import cn.projectan.strix.model.response.module.oss.OssBucketListResp;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.task.StrixOssTask;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ProjectAn
 * @date 2023/5/27 22:43
 */
@Slf4j
@RestController
@RequestMapping("system/oss/bucket")
@ConditionalOnBean(StrixOssConfig.class)
@RequiredArgsConstructor
public class OssBucketController extends BaseSystemController {

    private final OssBucketService ossBucketService;
    private final StrixOssTask strixOssTask;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "查询存储空间列表")
    public RetResult<OssBucketListResp> getOssBucketList(OssBucketListReq req) {
        QueryWrapper<OssBucket> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }

        Page<OssBucket> page = ossBucketService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new OssBucketListResp(page.getRecords(), page.getTotal()));
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:add')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "新增存储空间", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) OssBucketUpdateReq req) {
        OssBucket ossBucket = new OssBucket(
                req.getConfigKey(),
                req.getName(),
                null,
                null,
                null,
                req.getStorageClass(),
                null
        );
        ossBucket.setCreateBy(loginManagerId());
        ossBucket.setUpdateBy(loginManagerId());

        UniqueDetectionTool.check(ossBucket);

        ossBucketService.createBucket(ossBucket.getConfigKey(), ossBucket.getName(), ossBucket.getStorageClass());
        strixOssTask.refreshBucketList();

        // 使用同步进行创建

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:update')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "修改存储空间", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) OssBucketUpdateReq req) {
        OssBucket ossBucket = ossBucketService.getById(id);
        Assert.notNull(ossBucket, "原记录不存在");

        UpdateWrapper<OssBucket> updateWrapper = UpdateConditionBuilder.build(ossBucket, req);
        UniqueDetectionTool.check(ossBucket);
        Assert.isTrue(ossBucketService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:bucket:remove')")
    @StrixLog(operationGroup = "系统存储空间", operationName = "删除存储空间", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        ossBucketService.removeById(id);

        return RetMarker.makeSuccessRsp();
    }

}
