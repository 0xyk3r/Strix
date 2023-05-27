package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.system.StrixOssBucket;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.StorageClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author 安炯奕
 * @date 2023/5/22 15:37
 */
@Slf4j
public class AliyunOssClient extends StrixOssClient {

    /**
     * 公网 OSS 客户端
     */
    private OSS publicClient;

    /**
     * 私网 OSS 客户端
     */
    private OSS privateClient;

    public AliyunOssClient(OSS publicClient, OSS privateClient) {
        super();
        this.publicClient = publicClient;
        this.privateClient = privateClient;
    }

    @Override
    public Object getPublic() {
        return publicClient;
    }

    @Override
    public Object getPrivate() {
        return privateClient;
    }

    @Override
    public void uploadPublic(String bucketName, String objectName, byte[] buf) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
            publicClient.putObject(bucketName, objectName, byteArrayInputStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 上传文件失败.");
        }
    }

    @Override
    public void uploadPrivate(String bucketName, String objectName, byte[] buf) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
            privateClient.putObject(bucketName, objectName, byteArrayInputStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 上传文件失败.");
        }
    }

    @Override
    public File downloadPublic(String bucketName, String objectName, String filePath) {
        try {
            File file = new File(filePath);
            publicClient.getObject(new GetObjectRequest(bucketName, objectName), file);
            Assert.isTrue(file.exists(), "Strix OSS: 下载文件失败.");
            return file;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 下载文件失败.");
        }
    }

    @Override
    public File downloadPrivate(String bucketName, String objectName, String filePath) {
        try {
            File file = new File(filePath);
            privateClient.getObject(new GetObjectRequest(bucketName, objectName), file);
            Assert.isTrue(file.exists(), "Strix OSS: 下载文件失败.");
            return file;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 下载文件失败.");
        }
    }

    @Override
    public String getUrlPublic(String bucketName, String objectName, long expires) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + (expires * 1000));
            URL url = publicClient.generatePresignedUrl(bucketName, objectName, expiration);
            Assert.notNull(url, "Strix OSS: 获取文件URL失败.");
            return url.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 获取文件URL失败.");
        }
    }

    @Override
    public String getUrlPrivate(String bucketName, String objectName, long expires) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + (expires * 1000));
            URL url = privateClient.generatePresignedUrl(bucketName, objectName, expiration);
            Assert.notNull(url, "Strix OSS: 获取文件URL失败.");
            return url.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 获取文件URL失败.");
        }
    }

    @Override
    public void deletePublic(String bucketName, String objectName) {
        try {
            publicClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 删除文件失败.");
        }
    }

    @Override
    public void deletePrivate(String bucketName, String objectName) {
        try {
            privateClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 删除文件失败.");
        }
    }

    @Override
    public List<StrixOssBucket> getBucketList() {
        List<Bucket> buckets = privateClient.listBuckets();

        return Optional.ofNullable(buckets).orElse(Collections.emptyList()).stream().map(b ->
                new StrixOssBucket(
                        b.getName(),
                        b.getExtranetEndpoint(),
                        b.getIntranetEndpoint(),
                        b.getRegion(),
                        b.getStorageClass().toString(),
                        b.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                )).toList();
    }

    @Override
    public void createBucket(String bucketName, String storageClass) {
        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            if (StringUtils.hasText(storageClass)) {
                createBucketRequest.setStorageClass(StorageClass.parse(storageClass));
            }
            Bucket bucket = privateClient.createBucket(createBucketRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e.getMessage().contains("BucketAlreadyExists")) {
                throw new StrixException("Strix OSS: Bucket名称不可用，存储服务提供商要求Bucket名称不得与其他用户重复.");
            }
            throw new StrixException("Strix OSS: 创建Bucket失败.");
        }
    }

    @Override
    public void close() {
        if (publicClient != null) {
            publicClient.shutdown();
            publicClient = null;
        }
        if (privateClient != null) {
            privateClient.shutdown();
            privateClient = null;
        }
    }

}
