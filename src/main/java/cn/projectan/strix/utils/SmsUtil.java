package cn.projectan.strix.utils;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.core.sms.StrixSmsClient;
import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import cn.projectan.strix.service.SmsLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 短信发送工具
 *
 * @author 安炯奕
 * @date 2021/8/30 19:29
 */
@Slf4j
@Component
@ConditionalOnBean(StrixSmsConfig.class)
public class SmsUtil {

    @Autowired
    private SmsLogService smsLogService;
    @Autowired
    private StrixSmsConfig strixSmsConfig;

    public void send(SmsLog sms) {
        StrixSmsClient client = strixSmsConfig.getInstance(sms.getConfigKey());
        Assert.notNull(client, "Strix Sms: 发送短信失败. (短信服务实例不存在)");

        client.send(sms);
        smsLogService.save(sms);
    }

    public List<StrixSmsSign> getSignList(String configKey) {
        StrixSmsClient client = strixSmsConfig.getInstance(configKey);

        return client.getSignList();
    }

    public List<StrixSmsTemplate> getTemplateList(String configKey) {
        StrixSmsClient client = strixSmsConfig.getInstance(configKey);

        return client.getTemplateList();
    }

}
