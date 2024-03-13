package cn.projectan.strix.task;

import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssConfig;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.system.StrixOssBucket;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.utils.KeysDiffHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author ProjectAn
 * @date 2023/5/23 10:45
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(StrixOssConfig.class)
public class StrixOssTask {

    private final StrixOssConfig strixOssConfig;
    private final OssConfigService ossConfigService;
    private final OssBucketService ossBucketService;

    @Autowired
    public StrixOssTask(StrixOssConfig strixOssConfig, OssConfigService ossConfigService, OssBucketService ossBucketService) {
        this.strixOssConfig = strixOssConfig;
        this.ossConfigService = ossConfigService;
        this.ossBucketService = ossBucketService;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void refreshConfig() {
        List<OssConfig> ossConfigList = ossConfigService.list();
        List<String> ossConfigKeyList = ossConfigList.stream().map(OssConfig::getKey).toList();
        Set<String> instanceKeySet = strixOssConfig.getInstanceKeySet();

        KeysDiffHandler.handle(instanceKeySet, ossConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixOssConfig.getInstance(key)).ifPresent(StrixOssClient::close);
                    strixOssConfig.removeInstance(key);
                }),
                (addKeys) -> {
                    List<OssConfig> addSmsConfigList = ossConfigList.stream().filter(ossConfig -> addKeys.contains(ossConfig.getKey())).toList();
                    ossConfigService.createInstance(addSmsConfigList);
                });
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void refreshBucketList() {
        Set<String> instanceKeySet = strixOssConfig.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixOssBucket> bucketList = strixOssConfig.getInstance(key).getBucketList();
            ossBucketService.syncBucketList(key, bucketList);
        });
    }

}
