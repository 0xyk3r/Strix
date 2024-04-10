package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsTemplateMapper;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.utils.KeyDiffUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        List<SmsTemplate> dbTemplateList = this.list(new LambdaQueryWrapper<>(SmsTemplate.class).eq(SmsTemplate::getConfigKey, configKey));

        List<String> dbTemplateCodeList = dbTemplateList.stream().map(SmsTemplate::getCode).collect(Collectors.toList());
        List<String> templateCodeList = templateList.stream().map(StrixSmsTemplate::getCode).collect(Collectors.toList());

        KeyDiffUtil.handle(dbTemplateCodeList, templateCodeList,
                (removeKeys) -> {
                    QueryWrapper<SmsTemplate> removeQueryWrapper = new QueryWrapper<>();
                    removeQueryWrapper.eq("config_key", configKey);
                    removeQueryWrapper.in("code", removeKeys);
                    Assert.isTrue(remove(removeQueryWrapper), "Strix SMS: 同步删除模板失败.");
                },
                (addKeys) -> {
                    List<SmsTemplate> smsTemplateList = templateList.stream()
                            .filter(t -> addKeys.contains(t.getCode()))
                            .map(t -> new SmsTemplate(t.getCreateTime(), "System", null, "System")
                                    .setConfigKey(configKey)
                                    .setCode(t.getCode())
                                    .setName(t.getName())
                                    .setType(t.getType())
                                    .setStatus(t.getStatus())
                                    .setContent(t.getContent())
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(smsTemplateList), "Strix SMS: 同步增加模板失败.");
                }
        );
    }

}
