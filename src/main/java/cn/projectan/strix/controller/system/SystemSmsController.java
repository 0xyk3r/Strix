package cn.projectan.strix.controller.system;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.constant.StrixSmsPlatform;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.request.system.sms.SystemSmsConfigListQueryReq;
import cn.projectan.strix.model.request.system.sms.SystemSmsConfigUpdateReq;
import cn.projectan.strix.model.request.system.sms.SystemSmsSignListQueryReq;
import cn.projectan.strix.model.request.system.sms.SystemSmsTemplateListQueryReq;
import cn.projectan.strix.model.response.system.sms.SystemSmsConfigListQueryResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsConfigQueryByIdResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsSignListQueryResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsTemplateListQueryResp;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.task.StrixSmsTask;
import cn.projectan.strix.utils.*;
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
public class SystemSmsController extends BaseSystemController {

    @Autowired
    private SmsConfigService smsConfigService;
    @Autowired
    private SmsSignService smsSignService;
    @Autowired
    private SmsTemplateService smsTemplateService;

    @GetMapping("")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SystemSmsConfigListQueryResp> getSmsConfigList(SystemSmsConfigListQueryReq req) {
        QueryWrapper<SmsConfig> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("key", req.getKeyword())
                    .or(q -> q.like("name", req.getKeyword()));
        }

        Page<SmsConfig> page = smsConfigService.page(req.getPage(), queryWrapper);
        SystemSmsConfigListQueryResp resp = new SystemSmsConfigListQueryResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
    }


    @GetMapping("{id}")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SystemSmsConfigQueryByIdResp> getSmsConfigInfo(@PathVariable String id) {
        SmsConfig smsConfig = smsConfigService.getById(id);
        Assert.notNull(smsConfig, "短信配置不存在");

        List<SmsSign> signs = smsSignService.lambdaQuery().eq(SmsSign::getConfigKey, smsConfig.getKey()).list();
        List<SystemSmsSignListQueryResp.SmsSignItem> signItems = new SystemSmsSignListQueryResp(signs, (long) signs.size()).getSigns();

        List<SmsTemplate> templates = smsTemplateService.lambdaQuery().eq(SmsTemplate::getConfigKey, smsConfig.getKey()).list();
        List<SystemSmsTemplateListQueryResp.SmsTemplateItem> templateItems = new SystemSmsTemplateListQueryResp(templates, (long) templates.size()).getTemplates();

        return RetMarker.makeSuccessRsp(
                new SystemSmsConfigQueryByIdResp(
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
    public RetResult<Object> update(@RequestBody @Validated(ValidationGroup.Insert.class) SystemSmsConfigUpdateReq req) {
        StrixAssert.in(req.getPlatform(), "参数错误", StrixSmsPlatform.ALIYUN, StrixSmsPlatform.TENCENT);

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
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(ValidationGroup.Update.class) SystemSmsConfigUpdateReq req) {
        StrixAssert.in(req.getPlatform(), "参数错误", StrixSmsPlatform.ALIYUN, StrixSmsPlatform.TENCENT);

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
    public RetResult<SystemSmsSignListQueryResp> getSmsSignList(SystemSmsSignListQueryReq req) {
        QueryWrapper<SmsSign> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like("name", req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getStatus())) {
            queryWrapper.eq("status", req.getStatus());
        }

        Page<SmsSign> page = smsSignService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SystemSmsSignListQueryResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("template")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SystemSmsTemplateListQueryResp> getSmsTemplateList(SystemSmsTemplateListQueryReq req) {
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

        Page<SmsTemplate> page = smsTemplateService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SystemSmsTemplateListQueryResp(page.getRecords(), page.getTotal()));
    }

}
