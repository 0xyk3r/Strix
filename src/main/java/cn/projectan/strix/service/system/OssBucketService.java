package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.system.OssBucketMapper;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.other.system.module.oss.StrixOssBucket;
import cn.projectan.strix.model.request.system.module.oss.OssBucketListReq;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix OSS 容器 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssBucketService extends ServiceImpl<OssBucketMapper, OssBucket> {

    private final StrixOssStore strixOssStore;

    /**
     * 同步bucket列表
     *
     * @param configKey  配置key
     * @param bucketList bucket列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncBucketList(String configKey, List<StrixOssBucket> bucketList) {
        List<OssBucket> dbBucketList = lambdaQuery()
                .eq(OssBucket::getConfigKey, configKey)
                .list();

        List<String> dbBucketNameList = dbBucketList.stream().map(OssBucket::getName).collect(Collectors.toList());
        List<String> bucketNameList = bucketList.stream().map(StrixOssBucket::getName).collect(Collectors.toList());

        KeyDiffUtil.handle(dbBucketNameList, bucketNameList,
                (removeKeys) -> removeKeys.forEach(key ->
                        Assert.isTrue(
                                this.lambdaUpdate()
                                        .eq(OssBucket::getConfigKey, configKey)
                                        .in(OssBucket::getName, removeKeys)
                                        .remove(),
                                "Strix OSS: 同步删除存储空间失败.")),
                (addKeys) -> {
                    List<OssBucket> ossBucketList = bucketList.stream()
                            .filter(b -> addKeys.contains(b.getName()))
                            .map(b -> new OssBucket()
                                    .setConfigKey(configKey)
                                    .setName(b.getName())
                                    .setCreatedTime(b.getCreatedTime())
                                    .setCreatedByType(OperatorType.SYSTEM)
                                    .setUpdatedByType(OperatorType.SYSTEM)
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(ossBucketList), "Strix OSS: 同步增加存储空间失败.");
                }
        );
    }

    /**
     * 根据配置key查询bucket列表
     *
     * @param configKey 配置key
     * @return bucket列表
     */
    public List<OssBucket> listByConfigKey(String configKey) {
        return lambdaQuery()
                .eq(OssBucket::getConfigKey, configKey)
                .list();
    }

    /**
     * 根据配置key删除bucket
     *
     * @param configKey 配置key
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConfigKey(String configKey) {
        lambdaUpdate()
                .eq(OssBucket::getConfigKey, configKey)
                .remove();
    }

    /**
     * 分页查询存储空间列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<OssBucket> listPage(OssBucketListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssBucket::getName, req.getKeyword())
                .eq(StringUtils.hasText(req.getConfigKey()), OssBucket::getConfigKey, req.getConfigKey())
                .page(req.getPage());
    }

    /**
     * 创建bucket
     *
     * @param configKey  配置key
     * @param bucketName bucket名称
     */
    public void createBucket(String configKey, String bucketName) {
        StrixOssClient instance = strixOssStore.getInstance(configKey);
        Assert.notNull(instance, "创建存储空间失败. OSS服务配置不存在");
        instance.getPrivate().createBucket(bucketName);
    }

}
