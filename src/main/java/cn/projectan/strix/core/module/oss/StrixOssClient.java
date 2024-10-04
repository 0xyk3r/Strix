package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

/**
 * Strix OSS 客户端
 *
 * @author ProjectAn
 * @since 2023/5/22 15:21
 */
@Getter
@Setter
public abstract class StrixOssClient {

    public abstract Object getPublic();

    public abstract Object getPrivate();

    public abstract void uploadPublic(String bucketName, String objectName, byte[] buf);

    public abstract void uploadPrivate(String bucketName, String objectName, byte[] buf);

    public abstract File downloadPublic(String bucketName, String objectName, String filePath);

    public abstract File downloadPrivate(String bucketName, String objectName, String filePath);

    public abstract String getUrlPublic(String bucketName, String objectName, long expires);

    public abstract String getUrlPrivate(String bucketName, String objectName, long expires);

    public abstract void deletePublic(String bucketName, String objectName);

    public abstract void deletePrivate(String bucketName, String objectName);

    public abstract List<StrixOssBucket> getBucketList();

    public abstract void createBucket(String bucketName, String storageClass);

    public abstract void close();

}
