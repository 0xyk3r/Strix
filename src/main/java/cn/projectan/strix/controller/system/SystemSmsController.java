package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.request.system.sms.SystemSmsConfigListQueryReq;
import cn.projectan.strix.model.request.system.sms.SystemSmsSignListQueryReq;
import cn.projectan.strix.model.request.system.sms.SystemSmsTemplateListQueryReq;
import cn.projectan.strix.model.response.system.sms.SystemSmsConfigListQueryResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsConfigQueryByIdResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsSignListQueryResp;
import cn.projectan.strix.model.response.system.sms.SystemSmsTemplateListQueryResp;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.utils.NumUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            queryWrapper.like("id", req.getKeyword());
        }

        Page<SmsConfig> page = smsConfigService.page(req.getPage(), queryWrapper);
        SystemSmsConfigListQueryResp resp = new SystemSmsConfigListQueryResp(page.getRecords(), page.getTotal());

        return RetMarker.makeSuccessRsp(resp);
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

    @GetMapping("{smsConfigId}")
    @PreAuthorize("@ss.hasRead('System_Sms')")
    public RetResult<SystemSmsConfigQueryByIdResp> getSmsConfigInfo(@PathVariable String smsConfigId) {
        SmsConfig smsConfig = smsConfigService.getById(smsConfigId);
        Assert.notNull(smsConfig, "短信配置不存在");

        List<SmsSign> signs = smsSignService.lambdaQuery().eq(SmsSign::getConfigId, smsConfigId).list();
        List<SystemSmsSignListQueryResp.SmsSignItem> signItems = new SystemSmsSignListQueryResp(signs, (long) signs.size()).getSigns();

        List<SmsTemplate> templates = smsTemplateService.lambdaQuery().eq(SmsTemplate::getConfigId, smsConfigId).list();
        List<SystemSmsTemplateListQueryResp.SmsTemplateItem> templateItems = new SystemSmsTemplateListQueryResp(templates, (long) templates.size()).getTemplates();

        return RetMarker.makeSuccessRsp(
                new SystemSmsConfigQueryByIdResp(
                        smsConfig.getId(),
                        smsConfig.getPlatform(),
                        smsConfig.getRegionId(),
                        smsConfig.getAccessKey(),
                        smsConfig.getCreateTime(),
                        signItems,
                        templateItems
                )
        );
    }

}
