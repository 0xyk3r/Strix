package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.system.StrixOssBucket;
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

    void syncBucketList(String configKey, List<StrixOssBucket> bucketList);

    void createBucket(String configKey, String bucketName, String storageClass);

}
