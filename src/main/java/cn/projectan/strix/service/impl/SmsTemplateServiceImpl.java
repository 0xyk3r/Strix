package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsTemplateMapper;
import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.utils.RelationDiffHandler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-20
 */
@Service
public class SmsTemplateServiceImpl extends ServiceImpl<SmsTemplateMapper, SmsTemplate> implements SmsTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateList(String configId, List<StrixSmsTemplate> templateList) {
        QueryWrapper<SmsTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_id", configId);
        List<SmsTemplate> dbTemplateList = this.list(queryWrapper);

        List<String> dbTemplateCodeList = dbTemplateList.stream().map(SmsTemplate::getCode).toList();
        List<String> templateCodeList = templateList.stream().map(StrixSmsTemplate::getCode).toList();

        RelationDiffHandler.handle(dbTemplateCodeList, templateCodeList, ((removeKeys, addKeys) -> {
            if (removeKeys.size() > 0) {
                QueryWrapper<SmsTemplate> removeQueryWrapper = new QueryWrapper<>();
                removeQueryWrapper.eq("config_id", configId);
                removeQueryWrapper.in("code", removeKeys);
                Assert.isTrue(remove(removeQueryWrapper), "Strix Sms: 同步删除模板失败.");
            }
            if (addKeys.size() > 0) {
                List<SmsTemplate> smsTemplateList = new ArrayList<>();
                addKeys.forEach(k -> {
                    StrixSmsTemplate strixSmsTemplate = templateList.stream().filter(s -> s.getCode().equals(k)).findFirst().get();
                    SmsTemplate smsTemplate = new SmsTemplate();
                    smsTemplate.setConfigId(configId);
                    smsTemplate.setCode(k);
                    smsTemplate.setName(strixSmsTemplate.getName());
                    smsTemplate.setType(strixSmsTemplate.getType());
                    smsTemplate.setStatus(strixSmsTemplate.getStatus());
                    smsTemplate.setContent(strixSmsTemplate.getContent());
                    smsTemplate.setCreateTime(strixSmsTemplate.getCreateTime());
                    smsTemplate.setCreateBy("System");
                    smsTemplate.setUpdateBy("System");
                    smsTemplateList.add(smsTemplate);
                });
                Assert.isTrue(saveBatch(smsTemplateList), "Strix Sms: 同步增加模板失败.");
            }
        }));
    }

}
