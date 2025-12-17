package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oss.AliyunOssClient;
import cn.projectan.strix.core.module.oss.LocalOssClient;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.OssConfigMapper;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.dict.StrixOssPlatform;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.task.StrixOssTask;
import cn.projectan.strix.util.SpringUtil;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.tempurl.TempUrlUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * OSS 配置 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssConfigServiceImpl extends ServiceImpl<OssConfigMapper, OssConfig> implements OssConfigService {

    @Value("${spring.profiles.active}")
    private String profiles;

    private final TempUrlUtil tempUrlUtil;
    private final StrixOssStore strixOssStore;

    @Override
    public void refreshConfig() {
        List<OssConfig> ossConfigList = list();
        List<String> ossConfigKeyList = ossConfigList.stream()
                .map(OssConfig::getKey)
                .collect(Collectors.toList());
        Set<String> instanceKeySet = strixOssStore.getInstanceKeySet();

        KeyDiffUtil.handle(instanceKeySet, ossConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixOssStore.getInstance(key)).ifPresent(StrixOssClient::close);
                    strixOssStore.removeInstance(key);
                }),
                (addKeys) -> {
                    List<OssConfig> addSmsConfigList = ossConfigList.stream().filter(ossConfig -> addKeys.contains(ossConfig.getKey())).collect(Collectors.toList());
                    createInstance(addSmsConfigList);
                });
    }

    @Override
    public void createInstance(List<OssConfig> ossConfigList) {
        StrixOssTask strixOssTask = SpringUtil.getBean(StrixOssTask.class);
        StrixOssStore strixOssStore = SpringUtil.getBean(StrixOssStore.class);

        for (OssConfig ossConfig : ossConfigList) {
            boolean success = true;
            try {
                switch (ossConfig.getPlatform()) {
                    case StrixOssPlatform.ALIYUN -> {
                        S3Client publicClient = S3Client.builder()
                                .endpointOverride(URI.create(ossConfig.getPublicEndpoint()))
                                .credentialsProvider(
                                        StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ossConfig.getAccessKey(), ossConfig.getAccessSecret())
                                        )
                                )
                                .region(Region.of(ossConfig.getRegion()))
                                .serviceConfiguration(
                                        S3Configuration.builder()
                                                .pathStyleAccessEnabled(false)
                                                .chunkedEncodingEnabled(false)
                                                .build()
                                )
                                .build();

                        S3Presigner publicPresigner = S3Presigner.builder()
                                .endpointOverride(URI.create(ossConfig.getPublicEndpoint()))
                                .credentialsProvider(
                                        StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ossConfig.getAccessKey(), ossConfig.getAccessSecret())
                                        )
                                )
                                .region(Region.of(ossConfig.getRegion()))
                                .serviceConfiguration(
                                        S3Configuration.builder()
                                                .pathStyleAccessEnabled(false)
                                                .chunkedEncodingEnabled(false)
                                                .build()
                                )
                                .build();

                        S3Client privateClient = publicClient;
                        S3Presigner privatePresigner = publicPresigner;
                        if ("prod".equals(profiles)) {
                            privateClient = S3Client.builder()
                                    .endpointOverride(URI.create(ossConfig.getPrivateEndpoint()))
                                    .credentialsProvider(
                                            StaticCredentialsProvider.create(
                                                    AwsBasicCredentials.create(ossConfig.getAccessKey(), ossConfig.getAccessSecret())
                                            )
                                    )
                                    .region(Region.of(ossConfig.getRegion()))
                                    .serviceConfiguration(
                                            S3Configuration.builder()
                                                    .pathStyleAccessEnabled(false)
                                                    .chunkedEncodingEnabled(false)
                                                    .build()
                                    )
                                    .build();

                            privatePresigner = S3Presigner.builder()
                                    .endpointOverride(URI.create(ossConfig.getPrivateEndpoint()))
                                    .credentialsProvider(
                                            StaticCredentialsProvider.create(
                                                    AwsBasicCredentials.create(ossConfig.getAccessKey(), ossConfig.getAccessSecret())
                                            )
                                    )
                                    .region(Region.of(ossConfig.getRegion()))
                                    .serviceConfiguration(
                                            S3Configuration.builder()
                                                    .pathStyleAccessEnabled(false)
                                                    .chunkedEncodingEnabled(false)
                                                    .build()
                                    )
                                    .build();
                        }

                        Assert.notNull(publicClient, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败.");
                        Assert.notNull(publicPresigner, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败.");
                        Assert.notNull(privateClient, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败.");
                        Assert.notNull(privatePresigner, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败.");
                        strixOssStore.addInstance(ossConfig.getKey(), new AliyunOssClient(publicClient, publicPresigner, privateClient, privatePresigner));
                    }
                    case StrixOssPlatform.TENCENT ->
                            throw new StrixException("Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败. (暂不支持腾讯云对象存储服务)");
                    case StrixOssPlatform.LOCAL ->
                            strixOssStore.addInstance(ossConfig.getKey(), new LocalOssClient(ossConfig.getPublicEndpoint(), ossConfig.getPrivateEndpoint(), tempUrlUtil));
                    default ->
                            throw new StrixException("Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败. (暂不支持该对象存储服务平台)");
                }
            } catch (Exception e) {
                success = false;
                log.error("Strix OSS: 初始化对象存储服务实例 <{}> 失败. (其他错误)", ossConfig.getKey(), e);
            }
            if (success) {
                log.info("Strix OSS: 初始化对象存储服务实例 <{}> 完成.", ossConfig.getKey());
            }
        }

        // 全部初始化完成后，进行下一步操作
        strixOssTask.refreshBucketList();
    }

    @Override
    public CommonSelectDataResp getSelectData() {
        List<OssConfig> ossConfigList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(ossConfigList, "key", "key", "name");
    }

}
