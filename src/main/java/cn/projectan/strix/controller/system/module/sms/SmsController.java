package cn.projectan.strix.controller.system.module.sms;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.db.system.SmsSign;
import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.sms.*;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.system.module.sms.*;
import cn.projectan.strix.service.system.SmsConfigService;
import cn.projectan.strix.service.system.SmsLogService;
import cn.projectan.strix.service.system.SmsSignService;
import cn.projectan.strix.service.system.SmsTemplateService;
import cn.projectan.strix.task.system.StrixSmsTask;
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

import java.util.List;

/**
 * 短信管理
 *
 * @author ProjectAn
 * @since 2023/5/20 19:02
 */
@Slf4j
@RestController
@RequestMapping("system/sms")
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
@Tag(name = "系统模块 - 短信管理")
public class SmsController extends BaseSystemController {

    private final SmsConfigService smsConfigService;
    private final SmsSignService smsSignService;
    private final SmsTemplateService smsTemplateService;
    private final SmsLogService smsLogService;
    private final StrixSmsTask strixSmsTask;

    /**
     * 查询短信配置列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信配置列表")
    @Operation(summary = "短信配置列表")
    public RetResult<SmsConfigListResp> getSmsConfigList(SmsConfigListReq req) {
        Page<SmsConfig> page = smsConfigService.listPage(req);

        SmsConfigListResp resp = new SmsConfigListResp(page.getRecords(), page.getTotal());
        return RetBuilder.success(resp);
    }

    /**
     * 查询短信配置信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信配置信息")
    @Operation(summary = "短信配置详情")
    public RetResult<SmsConfigResp> getSmsConfigInfo(@Parameter(description = "短信配置 ID") @PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "短信配置不存在");

        List<SmsSign> signs = smsSignService.listByConfigKey(smsConfig.getKey());
        List<SmsSignListResp.SmsSignItem> signItems = new SmsSignListResp(signs, (long) signs.size()).getSigns();

        List<SmsTemplate> templates = smsTemplateService.listByConfigKey(smsConfig.getKey());
        List<SmsTemplateListResp.SmsTemplateItem> templateItems = new SmsTemplateListResp(templates, (long) templates.size()).getTemplates();

        return RetBuilder.success(
                new SmsConfigResp(
                        smsConfig.getId(),
                        smsConfig.getKey(),
                        smsConfig.getName(),
                        smsConfig.getPlatform(),
                        smsConfig.getRegionId(),
                        smsConfig.getAccessKey(),
                        smsConfig.getRemark(),
                        smsConfig.getCreatedTime(),
                        signItems,
                        templateItems
                )
        );
    }

    /**
     * 新增短信配置
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config:add')")
    @StrixLog(operationGroup = "系统短信", operationName = "新增短信配置", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新增短信配置")
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SmsConfigUpdateReq req) {
        SmsConfig smsConfig = new SmsConfig(
                req.getKey(),
                req.getName(),
                req.getPlatform(),
                req.getRegionId(),
                req.getAccessKey(),
                req.getAccessSecret(),
                req.getRemark()
        );

        UniqueChecker.check(smsConfig);

        Assert.isTrue(smsConfigService.save(smsConfig), "保存失败");

        // 重新加载配置
        strixSmsTask.refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 修改短信配置
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config:update')")
    @StrixLog(operationGroup = "系统短信", operationName = "修改短信配置", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "编辑短信配置")
    public RetResult<Object> update(@Parameter(description = "短信配置 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) SmsConfigUpdateReq req) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");
        String originKey = smsConfig.getKey();

        LambdaUpdateWrapper<SmsConfig> updateWrapper = UpdateBuilder.build(smsConfig, req);
        UniqueChecker.check(smsConfig);
        Assert.isTrue(smsConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        smsConfigService.closeInstance(originKey);
        strixSmsTask.refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 删除短信配置
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config:remove')")
    @StrixLog(operationGroup = "系统短信", operationName = "删除短信配置", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除短信配置")
    public RetResult<Object> remove(@Parameter(description = "短信配置 ID") @PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");

        smsConfigService.deleteConfigWithRelations(smsConfig);

        return RetBuilder.success();
    }

    /**
     * 查询短信配置信息
     */
    @GetMapping("sign")
    @PreAuthorize("@ss.hasPermission('system:module:sms:sign')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信签名列表")
    @Operation(summary = "短信签名列表")
    public RetResult<SmsSignListResp> getSmsSignList(SmsSignListReq req) {
        Page<SmsSign> page = smsSignService.listPage(req);

        return RetBuilder.success(new SmsSignListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信模板列表
     */
    @GetMapping("template")
    @PreAuthorize("@ss.hasPermission('system:module:sms:template')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信模板列表")
    @Operation(summary = "短信模板列表")
    public RetResult<SmsTemplateListResp> getSmsTemplateList(SmsTemplateListReq req) {
        Page<SmsTemplate> page = smsTemplateService.listPage(req);

        return RetBuilder.success(new SmsTemplateListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信日志列表
     */
    @GetMapping("log")
    @PreAuthorize("@ss.hasPermission('system:module:sms:log')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信日志列表")
    @Operation(summary = "短信发送记录列表")
    public RetResult<SmsLogListResp> getSmsLogList(SmsLogListReq req) {
        Page<SmsLog> page = smsLogService.listPage(req);

        return RetBuilder.success(new SmsLogListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信配置下拉列表
     */
    @GetMapping("config/select")
    @Operation(summary = "获取短信配置下拉列表")
    public RetResult<CommonSelectDataResp> getSmsConfigSelectList() {
        return RetBuilder.success(smsConfigService.getSelectData());
    }

}
