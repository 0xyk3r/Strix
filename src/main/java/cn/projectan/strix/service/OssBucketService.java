package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-23
 */
public interface OssBucketService extends IService<OssBucket> {

    /**
     * 同步bucket列表
     *
     * @param configKey  配置key
     * @param bucketList bucket列表
     */
    void syncBucketList(String configKey, List<StrixOssBucket> bucketList);

    /**
     * 创建bucket
     *
     * @param configKey    配置key
     * @param bucketName   bucket名称
     * @param storageClass 存储类型
     */
    void createBucket(String configKey, String bucketName, String storageClass);

}
