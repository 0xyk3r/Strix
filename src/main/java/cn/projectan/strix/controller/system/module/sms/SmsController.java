package cn.projectan.strix.controller.system.module.sms;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.constant.StrixSmsPlatform;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.request.module.sms.*;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.module.sms.*;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.service.SmsLogService;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.task.StrixSmsTask;
import cn.projectan.strix.utils.NumUtils;
import cn.projectan.strix.utils.SpringUtil;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/20 19:02
 */
@Slf4j
@RestController
@RequestMapping("system/sms")
public class SmsController extends BaseSystemController {

    @Autowired
    private SmsConfigService smsConfigService;
    @Autowired
    private SmsSignService smsSignService;
    @Autowired
    private SmsTemplateService smsTemplateService;
    @Autowired
    private SmsLogService smsLogService;

    @GetMapping("")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SmsConfigListResp> getSmsConfigList(SmsConfigListReq req) {
        QueryWrapper<SmsConfig> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("`key`", req.getKeyword())
                    .or(q -> q.like("`name`", req.getKeyword()));
        }

        Page<SmsConfig> page = smsConfigService.page(req.getPage(), queryWrapper);
        SmsConfigListResp resp = new SmsConfigListResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }


    @GetMapping("{id}")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SmsConfigResp> getSmsConfigInfo(@PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "短信配置不存在");

        List<SmsSign> signs = smsSignService.lambdaQuery().eq(SmsSign::getConfigKey, smsConfig.getKey()).list();
        List<SmsSignListResp.SmsSignItem> signItems = new SmsSignListResp(signs, (long) signs.size()).getSigns();

        List<SmsTemplate> templates = smsTemplateService.lambdaQuery().eq(SmsTemplate::getConfigKey, smsConfig.getKey()).list();
        List<SmsTemplateListResp.SmsTemplateItem> templateItems = new SmsTemplateListResp(templates, (long) templates.size()).getTemplates();

        return RetMarker.makeSuccessRsp(
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

    @PostMapping("update")
    @PreAuthorize("@ss.hasWrite('System_Sms')")
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SmsConfigUpdateReq req) {
        Assert.isTrue(StrixSmsPlatform.valid(req.getPlatform()), "请选择正确的服务平台");

        SmsConfig smsConfig = new SmsConfig(
                req.getKey(),
                req.getName(),
                req.getPlatform(),
                req.getRegionId(),
                req.getAccessKey(),
                req.getAccessSecret(),
                req.getRemark()
        );
        smsConfig.setCreateBy(getLoginManagerId());
        smsConfig.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(smsConfig);

        Assert.isTrue(smsConfigService.save(smsConfig), "保存失败");

        // 重新加载配置
        SpringUtil.getBean(StrixSmsTask.class).refreshConfig();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasWrite('System_Sms')")
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) SmsConfigUpdateReq req) {
        Assert.isTrue(StrixSmsPlatform.valid(req.getPlatform()), "请选择正确的服务平台");

        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");
        String originKey = smsConfig.getKey();

        UpdateWrapper<SmsConfig> updateWrapper = UpdateConditionBuilder.build(smsConfig, req, getLoginManagerId());
        UniqueDetectionTool.check(smsConfig);
        Assert.isTrue(smsConfigService.update(updateWrapper), "保存失败");

        // 卸载原配置 重新加载
        SpringUtil.getBean(StrixSmsConfig.class).getInstance(originKey).close();
        SpringUtil.getBean(StrixSmsTask.class).refreshConfig();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasWrite('System_Sms')")
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "原记录不存在");
        String key = smsConfig.getKey();

        smsConfigService.removeById(id);

        // 删除短信签名和模板
        smsSignService.remove(new LambdaQueryWrapper<>(SmsSign.class).eq(SmsSign::getConfigKey, key));
        smsTemplateService.remove(new LambdaQueryWrapper<>(SmsTemplate.class).eq(SmsTemplate::getConfigKey, key));

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("sign")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SmsSignListResp> getSmsSignList(SmsSignListReq req) {
        QueryWrapper<SmsSign> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getStatus())) {
            queryWrapper.eq("status", req.getStatus());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }

        Page<SmsSign> page = smsSignService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SmsSignListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("template")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SmsTemplateListResp> getSmsTemplateList(SmsTemplateListReq req) {
        QueryWrapper<SmsTemplate> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getType())) {
            queryWrapper.eq("type", req.getType());
        }
        if (NumUtils.isPositiveNumber(req.getStatus())) {
            queryWrapper.eq("status", req.getStatus());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }

        Page<SmsTemplate> page = smsTemplateService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SmsTemplateListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("log")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SmsLogListResp> getSmsLogList(SmsLogListReq req) {
        QueryWrapper<SmsLog> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("phone_number", req.getKeyword());
        }
        if (req.getStatus() != null) {
            queryWrapper.eq("status", req.getStatus());
        }
        if (StringUtils.hasText(req.getConfigKey())) {
            queryWrapper.eq("config_key", req.getConfigKey());
        }

        Page<SmsLog> page = smsLogService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SmsLogListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("config/select")
    public RetResult<CommonSelectDataResp> getSmsConfigSelectList() {
        return RetMarker.makeSuccessRsp(smsConfigService.getSelectData());
    }

}
