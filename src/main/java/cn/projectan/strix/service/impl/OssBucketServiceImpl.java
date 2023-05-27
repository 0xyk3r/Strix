package cn.projectan.strix.service.impl;

import cn.projectan.strix.config.StrixOssConfig;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.mapper.OssBucketMapper;
import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.system.StrixOssBucket;
import cn.projectan.strix.service.OssBucketService;
import cn.projectan.strix.utils.KeysDiffHandler;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-23
 */
@Service
public class OssBucketServiceImpl extends ServiceImpl<OssBucketMapper, OssBucket> implements OssBucketService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBucketList(String configKey, List<StrixOssBucket> bucketList) {
        List<OssBucket> dbBucketList = this.list(new LambdaQueryWrapper<>(OssBucket.class).eq(OssBucket::getConfigKey, configKey));

        List<String> dbBucketNameList = dbBucketList.stream().map(OssBucket::getName).toList();
        List<String> bucketNameList = bucketList.stream().map(StrixOssBucket::getName).toList();

        KeysDiffHandler.handle(dbBucketNameList, bucketNameList, ((removeKeys, addKeys) -> {
            if (removeKeys.size() > 0) {
                QueryWrapper<OssBucket> removeQueryWrapper = new QueryWrapper<>();
                removeQueryWrapper.eq("config_key", configKey);
                removeQueryWrapper.in("name", removeKeys);
                Assert.isTrue(remove(removeQueryWrapper), "Strix OSS: 同步删除存储空间失败.");
            }
            if (addKeys.size() > 0) {
                List<OssBucket> ossBucketList = new ArrayList<>();
                addKeys.forEach(k -> {
                    StrixOssBucket strixOssBucket = bucketList.stream().filter(s -> s.getName().equals(k)).findFirst().get();
                    OssBucket ossBucket = new OssBucket();
                    ossBucket.setConfigKey(configKey);
                    ossBucket.setName(strixOssBucket.getName());
                    ossBucket.setPublicEndpoint(strixOssBucket.getPublicEndpoint());
                    ossBucket.setPrivateEndpoint(strixOssBucket.getPrivateEndpoint());
                    ossBucket.setRegion(strixOssBucket.getRegion());
                    ossBucket.setStorageClass(strixOssBucket.getStorageClass());
                    ossBucket.setCreateTime(strixOssBucket.getCreateTime());
                    ossBucket.setCreateBy("System");
                    ossBucket.setUpdateBy("System");
                    ossBucketList.add(ossBucket);
                });
                Assert.isTrue(saveBatch(ossBucketList), "Strix OSS: 同步增加存储空间失败.");
            }
        }));
    }

    @Override
    public void createBucket(String configKey, String bucketName, String storageClass) {
        StrixOssClient instance = SpringUtil.getBean(StrixOssConfig.class).getInstance(configKey);
        Assert.notNull(instance, "创建存储空间失败. OSS服务配置不存在");
        instance.createBucket(bucketName, storageClass);
    }

}
