package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.constant.SysLogOperType;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.request.module.oss.OssFileGroupListReq;
import cn.projectan.strix.model.request.module.oss.OssFileGroupUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.module.oss.OssFileGroupListResp;
import cn.projectan.strix.model.response.module.oss.OssFileGroupResp;
import cn.projectan.strix.service.OssFileGroupService;
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

/**
 * @author 安炯奕
 * @date 2023/5/27 22:13
 */
@Slf4j
@RestController
@RequestMapping("system/oss/fileGroup")
public class OssFileGroupController extends BaseSystemController {

    @Autowired
    private OssFileGroupService ossFileGroupService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup')")
    @SysLog(operationGroup = "系统存储分组", operationName = "查询存储分组列表")
    public RetResult<OssFileGroupListResp> getOssFileGroupList(OssFileGroupListReq req) {
        QueryWrapper<OssFileGroup> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }

        Page<OssFileGroup> page = ossFileGroupService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new OssFileGroupListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup')")
    @SysLog(operationGroup = "系统存储分组", operationName = "查询存储分组信息")
    public RetResult<OssFileGroupResp> getOssFileGroupInfo(@PathVariable String id) {
        OssFileGroup ossFileGroup = ossFileGroupService.getById(id);
        Assert.notNull(ossFileGroup, "记录不存在");

        return RetMarker.makeSuccessRsp(new OssFileGroupResp(
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

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:add')")
    @SysLog(operationGroup = "系统存储分组", operationName = "新增存储分组", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) OssFileGroupUpdateReq req) {
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
        ossFileGroup.setCreateBy(getLoginManagerId());
        ossFileGroup.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(ossFileGroup);

        Assert.isTrue(ossFileGroupService.save(ossFileGroup), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:update')")
    @SysLog(operationGroup = "系统存储分组", operationName = "修改存储分组", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) OssFileGroupUpdateReq req) {
        OssFileGroup ossFileGroup = ossFileGroupService.getById(id);
        Assert.notNull(ossFileGroup, "原记录不存在");

        UpdateWrapper<OssFileGroup> updateWrapper = UpdateConditionBuilder.build(ossFileGroup, req, getLoginManagerId());
        UniqueDetectionTool.check(ossFileGroup);
        Assert.isTrue(ossFileGroupService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:oss:filegroup:remove')")
    @SysLog(operationGroup = "系统存储分组", operationName = "删除存储分组", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        ossFileGroupService.removeById(id);

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping(value = {"select", "select/{configKey}"})
    public RetResult<CommonSelectDataResp> getOssFileGroupSelectList(@PathVariable(required = false) String configKey) {
        return RetMarker.makeSuccessRsp(ossFileGroupService.getSelectData(configKey));
    }

}
