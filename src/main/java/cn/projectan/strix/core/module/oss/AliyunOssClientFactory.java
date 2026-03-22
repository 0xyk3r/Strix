package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.dict.system.OssPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * 阿里云 OSS 客户端工厂（基于 S3 兼容接口）
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class AliyunOssClientFactory implements OssClientFactory {

    private final Environment environment;

    @Override
    public short supportedPlatform() {
        return OssPlatform.ALIYUN;
    }

    @Override
    public StrixOssClient createClient(OssConfig config) {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getAccessSecret())
        );
        Region region = Region.of(config.getRegion());
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(false)
                .chunkedEncodingEnabled(false)
                .build();

        S3Client publicClient = S3Client.builder()
                .endpointOverride(URI.create(config.getPublicEndpoint()))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .serviceConfiguration(s3Configuration)
                .build();

        S3Presigner publicPresigner = S3Presigner.builder()
                .endpointOverride(URI.create(config.getPublicEndpoint()))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .serviceConfiguration(s3Configuration)
                .build();

        S3Client privateClient = publicClient;
        S3Presigner privatePresigner = publicPresigner;

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            privateClient = S3Client.builder()
                    .endpointOverride(URI.create(config.getPrivateEndpoint()))
                    .credentialsProvider(credentialsProvider)
                    .region(region)
                    .serviceConfiguration(s3Configuration)
                    .build();

            privatePresigner = S3Presigner.builder()
                    .endpointOverride(URI.create(config.getPrivateEndpoint()))
                    .credentialsProvider(credentialsProvider)
                    .region(region)
                    .serviceConfiguration(s3Configuration)
                    .build();
        }

        Assert.notNull(publicClient, "S3 公网客户端初始化失败");
        Assert.notNull(publicPresigner, "S3 公网预签名器初始化失败");
        Assert.notNull(privateClient, "S3 内网客户端初始化失败");
        Assert.notNull(privatePresigner, "S3 内网预签名器初始化失败");

        return new AliyunOssClient(publicClient, publicPresigner, privateClient, privatePresigner);
    }

}
