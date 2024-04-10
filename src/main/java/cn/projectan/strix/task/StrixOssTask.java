package cn.projectan.strix.task;

import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.utils.KeyDiffUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @date 2023/5/23 10:45
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(StrixOssStore.class)
public class StrixOssTask {

    private final StrixOssStore strixOssStore;
    private final OssConfigService ossConfigService;
    private final OssBucketService ossBucketService;

    @Autowired
    public StrixOssTask(StrixOssStore strixOssStore, OssConfigService ossConfigService, OssBucketService ossBucketService) {
        this.strixOssStore = strixOssStore;
        this.ossConfigService = ossConfigService;
        this.ossBucketService = ossBucketService;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void refreshConfig() {
        List<OssConfig> ossConfigList = ossConfigService.list();
        List<String> ossConfigKeyList = ossConfigList.stream().map(OssConfig::getKey).collect(Collectors.toList());
        Set<String> instanceKeySet = strixOssStore.getInstanceKeySet();

        KeyDiffUtil.handle(instanceKeySet, ossConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixOssStore.getInstance(key)).ifPresent(StrixOssClient::close);
                    strixOssStore.removeInstance(key);
                }),
                (addKeys) -> {
                    List<OssConfig> addSmsConfigList = ossConfigList.stream().filter(ossConfig -> addKeys.contains(ossConfig.getKey())).collect(Collectors.toList());
                    ossConfigService.createInstance(addSmsConfigList);
                });
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void refreshBucketList() {
        Set<String> instanceKeySet = strixOssStore.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixOssBucket> bucketList = strixOssStore.getInstance(key).getBucketList();
            ossBucketService.syncBucketList(key, bucketList);
        });
    }

}
