package cn.projectan.strix.util.common;

import cn.projectan.strix.core.module.sms.StrixSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsTemplate;
import cn.projectan.strix.service.system.SmsLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 短信发送服务
 *
 * @author ProjectAn
 * @since 2021/8/30 19:29
 */
@Slf4j
@Component
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
public class SmsService {

    private final SmsLogService smsLogService;
    private final StrixSmsStore strixSmsStore;

    public void send(SmsLog sms) {
        StrixSmsClient client = strixSmsStore.getInstance(sms.getConfigKey());
        Assert.notNull(client, I18nUtil.get("assert.sms.sendFailed"));

        client.send(sms);
        smsLogService.save(sms);
    }

    public List<StrixSmsSign> getSignList(String configKey) {
        StrixSmsClient client = strixSmsStore.getInstance(configKey);
        Assert.notNull(client, I18nUtil.get("assert.sms.getSignListFailed"));

        return client.getSignList();
    }

    public List<StrixSmsTemplate> getTemplateList(String configKey) {
        StrixSmsClient client = strixSmsStore.getInstance(configKey);
        Assert.notNull(client, I18nUtil.get("assert.sms.getTemplateListFailed"));

        return client.getTemplateList();
    }

}
