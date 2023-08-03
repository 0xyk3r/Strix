package cn.projectan.strix.task;

import cn.projectan.strix.core.module.sms.StrixSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsConfig;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.service.SmsTemplateService;
import cn.projectan.strix.utils.KeysDiffHandler;
import cn.projectan.strix.utils.SmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author 安炯奕
 * @date 2023/5/20 17:53
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(StrixSmsConfig.class)
@RequiredArgsConstructor
public class StrixSmsTask {

    private final SmsUtil smsUtil;
    private final StrixSmsConfig strixSmsConfig;
    private final SmsConfigService smsConfigService;
    private final SmsSignService smsSignService;
    private final SmsTemplateService smsTemplateService;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void refreshConfig() {
        List<SmsConfig> smsConfigList = smsConfigService.list();
        List<String> smsConfigKeyList = smsConfigList.stream().map(SmsConfig::getKey).toList();
        Set<String> instanceKeySet = strixSmsConfig.getInstanceKeySet();

        KeysDiffHandler.handle(instanceKeySet, smsConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixSmsConfig.getInstance(key)).ifPresent(StrixSmsClient::close);
                    strixSmsConfig.removeInstance(key);
                }), (addKeys) -> {
                    List<SmsConfig> addSmsConfigList = smsConfigList.stream().filter(smsConfig -> addKeys.contains(smsConfig.getKey())).toList();
                    smsConfigService.createInstance(addSmsConfigList);
                });
    }


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
