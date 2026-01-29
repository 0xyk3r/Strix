package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.system.module.oss.StrixOssBucket;
import cn.projectan.strix.util.file.FileUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.*;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
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

    private S3Client publicClient;
    private S3Presigner publicPresigner;
    private final DefaultOperations publicOperations;
    private S3Client privateClient;
    private S3Presigner privatePresigner;
    private final DefaultOperations privateOperations;

    public AliyunOssClient(S3Client publicClient, S3Presigner publicPresigner, S3Client privateClient, S3Presigner privatePresigner) {
        super();
        this.publicClient = publicClient;
        this.publicPresigner = publicPresigner;
        this.publicOperations = new DefaultOperations(publicClient, publicPresigner);
        this.privateClient = privateClient;
        this.privatePresigner = privatePresigner;
        this.privateOperations = new DefaultOperations(privateClient, privatePresigner);
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
            publicClient.close();
            publicClient = null;
        }
        if (publicPresigner != null) {
            publicPresigner.close();
            publicPresigner = null;
        }
        if (privateClient != null) {
            privateClient.close();
            privateClient = null;
        }
        if (privatePresigner != null) {
            privatePresigner.close();
            privatePresigner = null;
        }
    }

    public static class DefaultOperations implements StrixOssClient.Operations {

        private final S3Client client;
        private final S3Presigner presigner;

        public DefaultOperations(S3Client client, S3Presigner presigner) {
            this.client = client;
            this.presigner = presigner;
        }

        @Override
        public void upload(String bucketName, String objectName, byte[] buf) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                client.putObject(putObjectRequest, RequestBody.fromBytes(buf));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, InputStream inputStream, long contentLength) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .contentLength(contentLength)
                        .build();
                client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        @Deprecated
        public void upload(String bucketName, String objectName, InputStream inputStream) {
            try {
                // 将流读入字节数组以获取准确的长度
                // 注意: 这种方式不适合大文件，推荐使用 upload(bucketName, objectName, inputStream, contentLength)
                byte[] bytes = FileUtil.toByteArray(inputStream);
                upload(bucketName, objectName, bytes);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, File file) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                client.putObject(putObjectRequest, RequestBody.fromFile(file));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public String signUploadUrl(String bucketName, String objectName, long expires) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMillis(expires))
                        .putObjectRequest(putObjectRequest)
                        .build();
                String url = presigner.presignPutObject(presignRequest).url().toString();
                Assert.hasText(url, "Strix OSS: 获取文件上传 URL 失败.");
                return url;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件上传 URL 失败.");
            }
        }

        @Override
        public File download(String bucketName, String objectName, String filePath) {
            try {
                File file = new File(filePath);
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                client.getObject(getObjectRequest, file.toPath());
                Assert.isTrue(file.exists(), "Strix OSS: 下载文件失败.");
                return file;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        public InputStream downloadAsStream(String bucketName, String objectName) {
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                return client.getObject(getObjectRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        public void downloadToStream(String bucketName, String objectName, OutputStream outputStream) {
            try (InputStream inputStream = downloadAsStream(bucketName, objectName)) {
                FileUtil.copy(inputStream, outputStream);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        @Deprecated
        public File downloadStream(String bucketName, String objectName, String filePath) {
            try {
                File file = new File(filePath);
                try (InputStream s3InputStream = downloadAsStream(bucketName, objectName);
                     FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    FileUtil.copy(s3InputStream, fileOutputStream);
                }
                Assert.isTrue(file.exists(), "Strix OSS: 流式下载文件失败.");
                return file;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 流式下载文件失败.");
            }
        }

        @Override
        public StreamingResponseBody downloadStream(String bucketName, String objectName, HttpServletResponse response) {
            // 获取文件元信息
            try {
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                HeadObjectResponse headObjectResponse = client.headObject(headObjectRequest);
                long fileSize = headObjectResponse.contentLength();
                // 设置响应头
                response.setContentLengthLong(fileSize);
                // 设置适当的 Content-Type
                String contentType = headObjectResponse.contentType();
                if (!StringUtils.hasText(contentType)) {
                    contentType = "application/octet-stream";
                }
                response.setContentType(contentType);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件元信息失败.");
            }

            return outputStream -> {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                try (ResponseInputStream<GetObjectResponse> s3Object = client.getObject(getObjectRequest)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = s3Object.read(buffer)) != -1) {
                        try {
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                        } catch (IOException writeException) {
                            log.warn("Strix OSS: 检测到客户端断开连接，停止下载. 原因: {}", writeException.getMessage());
                            try {
                                s3Object.close();
                            } catch (IOException closeException) {
                                log.debug("关闭S3输入流时出现异常: {}", closeException.getMessage());
                            }
                            return;
                        }
                    }
                } catch (IOException e) {
                    // 检查是否为客户端断开连接的常见错误
                    if (e.getMessage() != null &&
                            (e.getMessage().contains("Broken pipe") ||
                                    e.getMessage().contains("Connection reset") ||
                                    e.getMessage().contains("中止") ||
                                    e.getMessage().contains("连接中断"))) {
                        log.warn("Strix OSS: 流式下载文件失败, 客户端可能已断开连接. 原因: {}", e.getMessage());
                    } else {
                        log.error(e.getMessage(), e);
                        throw new StrixException("Strix OSS: 流式下载文件失败.");
                    }
                }
            };
        }

        @Override
        public String signDownloadUrl(String bucketName, String objectName, long expires) {
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMillis(expires))
                        .getObjectRequest(getObjectRequest)
                        .build();
                String url = presigner.presignGetObject(presignRequest).url().toString();
                Assert.hasText(url, "Strix OSS: 获取文件下载 URL 失败.");
                return url;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件下载 URL 失败.");
            }
        }

        @Override
        public boolean exist(String bucketName, String objectName) {
            try {
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                client.headObject(headObjectRequest);
                return true;
            } catch (NoSuchKeyException e) {
                return false;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 判断文件是否存在失败.");
            }
        }

        @Override
        public void list(String bucketName, String prefix, int maxKeys) {
            try {
                String continuationToken = null;
                ListObjectsV2Response result;
                do {
                    ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .maxKeys(maxKeys);
                    if (StringUtils.hasText(prefix)) {
                        builder.prefix(prefix);
                    }
                    if (continuationToken != null) {
                        builder.continuationToken(continuationToken);
                    }
                    result = client.listObjectsV2(builder.build());

                    List<S3Object> contents = result.contents();
                    for (S3Object s : contents) {
                        System.out.println("\t" + s.key());
                    }

                    continuationToken = result.nextContinuationToken();

                } while (Boolean.TRUE.equals(result.isTruncated()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取文件列表失败.");
            }
        }

        @Override
        public void delete(String bucketName, String objectName) {
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build();
                client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 删除文件失败.");
            }
        }

        @Override
        public List<StrixOssBucket> listBuckets() {
            try {
                ListBucketsResponse response = client.listBuckets();
                List<Bucket> buckets = response.buckets();
                return Optional.ofNullable(buckets).orElse(Collections.emptyList()).stream().map(b ->
                        new StrixOssBucket(
                                b.name(),
                                b.creationDate().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        )).collect(Collectors.toList());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 获取桶列表失败.");
            }
        }

        @Override
        public void createBucket(String bucketName) {
            try {
                CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                        .bucket(bucketName);
                client.createBucket(builder.build());
            } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: Bucket名称不可用，存储服务提供商要求Bucket名称不得与其他用户重复.");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 创建Bucket失败.");
            }
        }

        @Override
        public void deleteBucket(String bucketName) {
            try {
                DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                client.deleteBucket(deleteBucketRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 删除Bucket失败.");
            }
        }
    }

}
