package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsTemplateMapper;
import cn.projectan.strix.model.constant.OperatorType;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
@Service
public class SmsTemplateServiceImpl extends ServiceImpl<SmsTemplateMapper, SmsTemplate> implements SmsTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateList(String configKey, List<StrixSmsTemplate> templateList) {
        List<SmsTemplate> dbTemplateList = lambdaQuery()
                .eq(SmsTemplate::getConfigKey, configKey)
                .list();

        List<String> dbTemplateCodeList = dbTemplateList.stream().map(SmsTemplate::getCode).collect(Collectors.toList());
        List<String> templateCodeList = templateList.stream().map(StrixSmsTemplate::getCode).collect(Collectors.toList());

        KeyDiffUtil.handle(dbTemplateCodeList, templateCodeList,
                (removeKeys) -> {
                    Assert.isTrue(
                            this.lambdaUpdate()
                                    .eq(SmsTemplate::getConfigKey, configKey)
                                    .in(SmsTemplate::getCode, removeKeys)
                                    .remove(),
                            "Strix SMS: 同步删除模板失败.");
                },
                (addKeys) -> {
                    List<SmsTemplate> smsTemplateList = templateList.stream()
                            .filter(t -> addKeys.contains(t.getCode()))
                            .map(t -> new SmsTemplate()
                                    .setConfigKey(configKey)
                                    .setCode(t.getCode())
                                    .setName(t.getName())
                                    .setType(t.getType())
                                    .setStatus(t.getStatus())
                                    .setContent(t.getContent())
                                    .setCreatedTime(t.getCreatedTime())
                                    .setCreatedByType(OperatorType.SYSTEM)
                                    .setUpdatedByType(OperatorType.SYSTEM)
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(smsTemplateList), "Strix SMS: 同步增加模板失败.");
                }
        );
    }

}
