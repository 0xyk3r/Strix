package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 阿里云 OSS 客户端
 *
 * @author ProjectAn
 * @since 2023/5/22 15:37
 */
@Slf4j
public class AliyunOssClient implements StrixOssClient {

    private OSS publicClient;
    private final DefaultOperations publicOperations;
    private OSS privateClient;
    private final DefaultOperations privateOperations;

    public AliyunOssClient(OSS publicClient, OSS privateClient) {
        super();
        this.publicClient = publicClient;
        this.publicOperations = new DefaultOperations(publicClient);
        this.privateClient = privateClient;
        this.privateOperations = new DefaultOperations(privateClient);
    }

    @Override
    public DefaultOperations getPublic() {
        return publicOperations;
    }

    @Override
    public DefaultOperations getPrivate() {
        return privateOperations;
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

    public static class DefaultOperations implements StrixOssClient.Operations {

        private final OSS client;

        public DefaultOperations(OSS client) {
            this.client = client;
        }

        @Override
        public void upload(String bucketName, String objectName, byte[] buf) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
                client.putObject(bucketName, objectName, byteArrayInputStream);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, InputStream inputStream) {
            try {
                client.putObject(bucketName, objectName, inputStream);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, File file) {
            try {
                client.putObject(bucketName, objectName, file);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public String signUploadUrl(String bucketName, String objectName, long expires) {
            try {
                Date expiration = new Date(System.currentTimeMillis() + expires);
                URL url = client.generatePresignedUrl(bucketName, objectName, expiration, HttpMethod.PUT);
                Assert.notNull(url, "Strix OSS: 获取文件上传 URL 失败.");
                return url.toString();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件上传 URL 失败.");
            }
        }

        @Override
        public File download(String bucketName, String objectName, String filePath) {
            try {
                File file = new File(filePath);
                client.getObject(new GetObjectRequest(bucketName, objectName), file);
                Assert.isTrue(file.exists(), "Strix OSS: 下载文件失败.");
                return file;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        public File downloadStream(String bucketName, String objectName, String filePath) {
            try {
                File file = new File(filePath);
                try (OSSObject ossObject = client.getObject(bucketName, objectName);
                     InputStream inputStream = ossObject.getObjectContent();
                     FileOutputStream fileOutputStream = new FileOutputStream(file)
                ) {
                    // 读取文件内容到字节数组。
                    byte[] readBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(readBuffer)) != -1) {
                        fileOutputStream.write(readBuffer, 0, bytesRead);
                    }
                }
                Assert.isTrue(file.exists(), "Strix OSS: 流式下载文件失败.");
                return file;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 流式下载文件失败.");
            }
        }

        @Override
        public String signDownloadUrl(String bucketName, String objectName, long expires) {
            try {
                Date expiration = new Date(System.currentTimeMillis() + expires);
                URL url = client.generatePresignedUrl(bucketName, objectName, expiration);
                Assert.notNull(url, "Strix OSS: 获取文件下载 URL 失败.");
                return url.toString();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件下载 URL 失败.");
            }
        }

        @Override
        public boolean exist(String bucketName, String objectName) {
            try {
                return client.doesObjectExist(bucketName, objectName);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 判断文件是否存在失败.");
            }
        }

        @Override
        public void list(String bucketName, String prefix, int maxKeys) {
            try {
                String nextContinuationToken = null;
                ListObjectsV2Result result = null;
                do {
                    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request(bucketName).withMaxKeys(maxKeys);
                    listObjectsV2Request.setPrefix(prefix);
                    listObjectsV2Request.setContinuationToken(nextContinuationToken);
                    result = client.listObjectsV2(listObjectsV2Request);

                    List<OSSObjectSummary> sums = result.getObjectSummaries();
                    for (OSSObjectSummary s : sums) {
                        System.out.println("\t" + s.getKey());
                    }

                    nextContinuationToken = result.getNextContinuationToken();

                } while (result.isTruncated());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件列表失败.");
            }
        }

        @Override
        public void delete(String bucketName, String objectName) {
            try {
                client.deleteObject(bucketName, objectName);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 删除文件失败.");
            }
        }

        @Override
        public List<StrixOssBucket> listBuckets() {
            try {
                List<Bucket> buckets = client.listBuckets();
                return Optional.ofNullable(buckets).orElse(Collections.emptyList()).stream().map(b ->
                        new StrixOssBucket(
                                b.getName(),
                                b.getExtranetEndpoint(),
                                b.getIntranetEndpoint(),
                                b.getRegion(),
                                b.getStorageClass().toString(),
                                b.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        )).collect(Collectors.toList());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取桶列表失败.");
            }
        }

        @Override
        public void createBucket(String bucketName, String storageClass) {
            try {
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
                if (StringUtils.hasText(storageClass)) {
                    createBucketRequest.setStorageClass(StorageClass.parse(storageClass));
                }
                client.createBucket(createBucketRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (e.getMessage().contains("BucketAlreadyExists")) {
                    throw new StrixException("Strix OSS: Bucket名称不可用，存储服务提供商要求Bucket名称不得与其他用户重复.");
                }
                throw new StrixException("Strix OSS: 创建Bucket失败.");
            }
        }

        @Override
        public void deleteBucket(String bucketName) {
            try {
                client.deleteBucket(bucketName);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 删除Bucket失败.");
            }
        }
    }

}
