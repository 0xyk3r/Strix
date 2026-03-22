package cn.projectan.strix.task.system;

import cn.projectan.strix.core.module.sms.StrixSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsTemplate;
import cn.projectan.strix.service.system.SmsConfigService;
import cn.projectan.strix.service.system.SmsSignService;
import cn.projectan.strix.service.system.SmsTemplateService;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.common.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Strix SMS 任务
 *
 * @author ProjectAn
 * @since 2023/5/20 17:53
 */
@Slf4j
@Component
@ConditionalOnBean(StrixSmsStore.class)
@RequiredArgsConstructor
public class StrixSmsTask {

    private final SmsService smsService;
    private final StrixSmsStore strixSmsStore;
    private final SmsConfigService smsConfigService;
    private final SmsSignService smsSignService;
    private final SmsTemplateService smsTemplateService;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void refreshConfig() {
        List<SmsConfig> smsConfigList = smsConfigService.list();
        List<String> smsConfigKeyList = smsConfigList.stream()
                .map(SmsConfig::getKey)
                .collect(Collectors.toList());
        Set<String> instanceKeySet = strixSmsStore.getInstanceKeySet();

        KeyDiffUtil.handle(instanceKeySet, smsConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixSmsStore.getInstance(key)).ifPresent(StrixSmsClient::close);
                    strixSmsStore.removeInstance(key);
                }), (addKeys) -> {
                    List<SmsConfig> addSmsConfigList = smsConfigList.stream().filter(smsConfig -> addKeys.contains(smsConfig.getKey())).collect(Collectors.toList());
                    smsConfigService.createInstance(addSmsConfigList);
                });
    }


    @Scheduled(cron = "0 10 0 * * ?")
    public void refreshSignList() {
        Set<String> instanceKeySet = strixSmsStore.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixSmsSign> signList = smsService.getSignList(key);
            smsSignService.syncSignList(key, signList);
        });
    }

    @Scheduled(cron = "0 20 0 * * ?")
    public void refreshTemplateList() {
        Set<String> instanceKeySet = strixSmsStore.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixSmsTemplate> templateList = smsService.getTemplateList(key);
            smsTemplateService.syncTemplateList(key, templateList);
        });
    }

}
