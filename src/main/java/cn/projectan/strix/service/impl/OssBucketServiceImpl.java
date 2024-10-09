package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.OssBucketMapper;
import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.util.SpringUtil;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-23
 */
@Service
public class OssBucketServiceImpl extends ServiceImpl<OssBucketMapper, OssBucket> implements OssBucketService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBucketList(String configKey, List<StrixOssBucket> bucketList) {
        List<OssBucket> dbBucketList = lambdaQuery()
                .eq(OssBucket::getConfigKey, configKey)
                .list();

        List<String> dbBucketNameList = dbBucketList.stream().map(OssBucket::getName).collect(Collectors.toList());
        List<String> bucketNameList = bucketList.stream().map(StrixOssBucket::getName).collect(Collectors.toList());

        KeyDiffUtil.handle(dbBucketNameList, bucketNameList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Assert.isTrue(
                            this.lambdaUpdate()
                                    .eq(OssBucket::getConfigKey, configKey)
                                    .in(OssBucket::getName, removeKeys)
                                    .remove(),
                            "Strix OSS: 同步删除存储空间失败.");
                }),
                (addKeys) -> {
                    List<OssBucket> ossBucketList = bucketList.stream()
                            .filter(b -> addKeys.contains(b.getName()))
                            .map(b -> new OssBucket()
                                    .setConfigKey(configKey)
                                    .setName(b.getName())
                                    .setPublicEndpoint(b.getPublicEndpoint())
                                    .setPrivateEndpoint(b.getPrivateEndpoint())
                                    .setRegion(b.getRegion())
                                    .setStorageClass(b.getStorageClass())
                                    .setCreateTime(b.getCreateTime())
                                    .setCreateBy("SYSTEM")
                                    .setUpdateBy("SYSTEM")
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(ossBucketList), "Strix OSS: 同步增加存储空间失败.");
                }
        );
    }

    @Override
    public void createBucket(String configKey, String bucketName, String storageClass) {
        StrixOssClient instance = SpringUtil.getBean(StrixOssStore.class).getInstance(configKey);
        Assert.notNull(instance, "创建存储空间失败. OSS服务配置不存在");
        instance.createBucket(bucketName, storageClass);
    }

}
