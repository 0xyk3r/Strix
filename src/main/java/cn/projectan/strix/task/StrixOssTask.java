package cn.projectan.strix.task;

import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.service.OssConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Strix OSS 任务
 *
 * @author ProjectAn
 * @since 2023/5/23 10:45
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
        ossConfigService.refreshConfig();
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void refreshBucketList() {
        Set<String> instanceKeySet = strixOssStore.getInstanceKeySet();
        instanceKeySet.forEach(key -> {
            List<StrixOssBucket> bucketList = strixOssStore.getInstance(key).getPrivate().listBuckets();
            ossBucketService.syncBucketList(key, bucketList);
        });
    }

}
