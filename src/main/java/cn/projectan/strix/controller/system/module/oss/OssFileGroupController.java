package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.module.oss.OssFileGroupListReq;
import cn.projectan.strix.model.request.module.oss.OssFileGroupUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.module.oss.OssFileGroupListResp;
import cn.projectan.strix.model.response.module.oss.OssFileGroupResp;
import cn.projectan.strix.service.OssFileGroupService;
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

/**
 * 系统存储分组
 *
 * @author ProjectAn
 * @date 2023/5/27 22:13
 */
@Slf4j
@RestController
@RequestMapping("system/oss/fileGroup")
@ConditionalOnBean(StrixOssStore.class)
@RequiredArgsConstructor
public class OssFileGroupController extends BaseSystemController {

    private final OssFileGroupService ossFileGroupService;

    /**
     * 查询存储分组列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup')")
    @StrixLog(operationGroup = "系统存储分组", operationName = "查询存储分组列表")
    public RetResult<OssFileGroupListResp> getOssFileGroupList(OssFileGroupListReq req) {
        Page<OssFileGroup> page = ossFileGroupService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssFileGroup::getName, req.getKeyword())
                .eq(StringUtils.hasText(req.getConfigKey()), OssFileGroup::getConfigKey, req.getConfigKey())
                .page(req.getPage());

        return RetBuilder.success(new OssFileGroupListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询存储分组信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup')")
    @StrixLog(operationGroup = "系统存储分组", operationName = "查询存储分组信息")
    public RetResult<OssFileGroupResp> getOssFileGroupInfo(@PathVariable String id) {
        OssFileGroup ossFileGroup = ossFileGroupService.getById(id);
        Assert.notNull(ossFileGroup, "记录不存在");

        return RetBuilder.success(new OssFileGroupResp(
                ossFileGroup.getId(),
                ossFileGroup.getKey(),
                ossFileGroup.getConfigKey(),
                ossFileGroup.getName(),
                ossFileGroup.getBucketName(),
                ossFileGroup.getBucketDomain(),
                ossFileGroup.getBaseDir(),
                ossFileGroup.getAllowExtension(),
                ossFileGroup.getSecretType(),
                ossFileGroup.getSecretLevel(),
                ossFileGroup.getRemark(),
                ossFileGroup.getCreateTime()
        ));
    }

    /**
     * 新增存储分组
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:add')")
    @StrixLog(operationGroup = "系统存储分组", operationName = "新增存储分组", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) OssFileGroupUpdateReq req) {
        OssFileGroup ossFileGroup = new OssFileGroup(
                req.getKey(),
                req.getConfigKey(),
                req.getName(),
                req.getBucketName(),
                req.getBucketDomain(),
                req.getBaseDir(),
                req.getAllowExtension(),
                req.getSecretType(),
                req.getSecretLevel(),
                req.getRemark()
        );

        UniqueDetectionTool.check(ossFileGroup);

        Assert.isTrue(ossFileGroupService.save(ossFileGroup), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改存储分组
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:update')")
    @StrixLog(operationGroup = "系统存储分组", operationName = "修改存储分组", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) OssFileGroupUpdateReq req) {
        OssFileGroup ossFileGroup = ossFileGroupService.getById(id);
        Assert.notNull(ossFileGroup, "原记录不存在");

        LambdaUpdateWrapper<OssFileGroup> updateWrapper = UpdateConditionBuilder.build(ossFileGroup, req);
        UniqueDetectionTool.check(ossFileGroup);
        Assert.isTrue(ossFileGroupService.update(updateWrapper), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 删除存储分组
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:remove')")
    @StrixLog(operationGroup = "系统存储分组", operationName = "删除存储分组", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        ossFileGroupService.removeById(id);
        return RetBuilder.success();
    }

    /**
     * 获取存储分组下拉列表
     */
    @GetMapping(value = {"select", "select/{configKey}"})
    public RetResult<CommonSelectDataResp> getOssFileGroupSelectList(@PathVariable(required = false) String configKey) {
        return RetBuilder.success(ossFileGroupService.getSelectData(configKey));
    }

}
