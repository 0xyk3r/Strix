package cn.projectan.strix.task;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.utils.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author 安炯奕
 * @date 2023/5/20 17:53
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(StrixSmsConfig.class)
public class StrixSmsTask {

    @Autowired
    private SmsUtil smsUtil;
    @Autowired
    private StrixSmsConfig strixSmsConfig;
    @Autowired
    private SmsSignService smsSignService;
    @Autowired
    private SmsTemplateService smsTemplateService;

    @Scheduled(cron = "0 10 0 * * ?")
    public void refreshSignList() {
        Set<String> instanceKeySet = strixSmsConfig.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixSmsSign> signList = smsUtil.getSignList(key);
            smsSignService.syncSignList(key, signList);
        });
    }

    @Scheduled(cron = "0 20 0 * * ?")
    public void refreshTemplateList() {
        Set<String> instanceKeySet = strixSmsConfig.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixSmsTemplate> templateList = smsUtil.getTemplateList(key);
            smsTemplateService.syncTemplateList(key, templateList);
        });
    }

}
