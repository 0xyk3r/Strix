package cn.projectan.strix.utils;

import cn.projectan.strix.core.module.sms.StrixSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import cn.projectan.strix.service.SmsLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 短信发送工具
 *
 * @author ProjectAn
 * @date 2021/8/30 19:29
 */
@Slf4j
@Component
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
public class SmsUtil {

    private final SmsLogService smsLogService;
    private final StrixSmsStore strixSmsStore;

    public void send(SmsLog sms) {
        StrixSmsClient client = strixSmsStore.getInstance(sms.getConfigKey());
        Assert.notNull(client, "Strix SMS: 发送短信失败. (短信服务实例不存在)");

        client.send(sms);
        smsLogService.save(sms);
    }

    public List<StrixSmsSign> getSignList(String configKey) {
        StrixSmsClient client = strixSmsStore.getInstance(configKey);
        Assert.notNull(client, "Strix SMS: 获取短信签名列表失败. (短信服务实例不存在)");

        return client.getSignList();
    }

    public List<StrixSmsTemplate> getTemplateList(String configKey) {
        StrixSmsClient client = strixSmsStore.getInstance(configKey);
        Assert.notNull(client, "Strix SMS: 获取短信模板列表失败. (短信服务实例不存在)");

        return client.getTemplateList();
    }

}
