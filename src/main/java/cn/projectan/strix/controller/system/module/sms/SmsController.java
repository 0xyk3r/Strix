package cn.projectan.strix.controller.system.module.sms;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.enums.NumCategory;
import cn.projectan.strix.model.request.module.sms.*;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.module.sms.*;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.service.SmsLogService;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.task.StrixSmsTask;
import cn.projectan.strix.utils.NumUtil;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
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

import java.util.List;

/**
 * 短信管理
 *
 * @author ProjectAn
 * @date 2023/5/20 19:02
 */
@Slf4j
@RestController
@RequestMapping("system/sms")
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
public class SmsController extends BaseSystemController {

    private final SmsConfigService smsConfigService;
    private final SmsSignService smsSignService;
    private final SmsTemplateService smsTemplateService;
    private final SmsLogService smsLogService;

    /**
     * 查询短信配置列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信配置列表")
    public RetResult<SmsConfigListResp> getSmsConfigList(SmsConfigListReq req) {
        Page<SmsConfig> page = smsConfigService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsConfig::getKey, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SmsConfig::getName, req.getKeyword()))
                .page(req.getPage());

        SmsConfigListResp resp = new SmsConfigListResp(page.getRecords(), page.getTotal());
        return RetBuilder.success(resp);
    }

    /**
     * 查询短信配置信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信配置信息")
    public RetResult<SmsConfigResp> getSmsConfigInfo(@PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "短信配置不存在");

        List<SmsSign> signs = smsSignService.lambdaQuery()
                .eq(SmsSign::getConfigKey, smsConfig.getKey())
                .list();
        List<SmsSignListResp.SmsSignItem> signItems = new SmsSignListResp(signs, (long) signs.size()).getSigns();

        List<SmsTemplate> templates = smsTemplateService.lambdaQuery()
                .eq(SmsTemplate::getConfigKey, smsConfig.getKey())
                .list();
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
                        smsConfig.getCreateTime(),
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
    @StrixLog(operationGroup = "系统短信", operationName = "新增短信配置", operationType = SysLogOperType.ADD)
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

        UniqueDetectionTool.check(smsConfig);

        Assert.isTrue(smsConfigService.save(smsConfig), "保存失败");

        // 重新加载配置
        SpringUtil.getBean(StrixSmsTask.class).refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 修改短信配置
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config:update')")
    @StrixLog(operationGroup = "系统短信", operationName = "修改短信配置", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) SmsConfigUpdateReq req) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");
        String originKey = smsConfig.getKey();

        UpdateWrapper<SmsConfig> updateWrapper = UpdateConditionBuilder.build(smsConfig, req);
        UniqueDetectionTool.check(smsConfig);
        Assert.isTrue(smsConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        SpringUtil.getBean(StrixSmsStore.class).getInstance(originKey).close();
        SpringUtil.getBean(StrixSmsTask.class).refreshConfig();

        return RetBuilder.success();
    }

    /**
     * 删除短信配置
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:sms:config:remove')")
    @StrixLog(operationGroup = "系统短信", operationName = "删除短信配置", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");
        String key = smsConfig.getKey();

        smsConfigService.removeById(id);

        // 删除短信签名和模板
        smsSignService.lambdaUpdate()
                .eq(SmsSign::getConfigKey, key)
                .remove();
        smsTemplateService.lambdaUpdate()
                .eq(SmsTemplate::getConfigKey, key)
                .remove();

        return RetBuilder.success();
    }

    /**
     * 查询短信配置信息
     */
    @GetMapping("sign")
    @PreAuthorize("@ss.hasPermission('system:module:sms:sign')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信签名列表")
    public RetResult<SmsSignListResp> getSmsSignList(SmsSignListReq req) {
        Page<SmsSign> page = smsSignService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsSign::getName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsSign::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsSign::getConfigKey, req.getConfigKey())
                .page(req.getPage());

        return RetBuilder.success(new SmsSignListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信模板列表
     */
    @GetMapping("template")
    @PreAuthorize("@ss.hasPermission('system:module:sms:template')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信模板列表")
    public RetResult<SmsTemplateListResp> getSmsTemplateList(SmsTemplateListReq req) {
        Page<SmsTemplate> page = smsTemplateService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsTemplate::getName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getType(), NumCategory.POSITIVE), SmsTemplate::getType, req.getType())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsTemplate::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsTemplate::getConfigKey, req.getConfigKey())
                .page(req.getPage());

        return RetBuilder.success(new SmsTemplateListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信日志列表
     */
    @GetMapping("log")
    @PreAuthorize("@ss.hasPermission('system:module:sms:log')")
    @StrixLog(operationGroup = "系统短信", operationName = "查询短信日志列表")
    public RetResult<SmsLogListResp> getSmsLogList(SmsLogListReq req) {
        Page<SmsLog> page = smsLogService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsLog::getPhoneNumber, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsLog::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsLog::getConfigKey, req.getConfigKey())
                .page(req.getPage());

        return RetBuilder.success(new SmsLogListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询短信配置下拉列表
     */
    @GetMapping("config/select")
    public RetResult<CommonSelectDataResp> getSmsConfigSelectList() {
        return RetBuilder.success(smsConfigService.getSelectData());
    }

}
