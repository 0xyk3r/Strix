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
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
                        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(ossConfig.getAccessKey(), ossConfig.getAccessSecret());
                        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
                        clientBuilderConfiguration.setSupportCname(true);
                        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
                        OSS publicClient = OSSClientBuilder.create()
                                .endpoint(ossConfig.getPublicEndpoint())
                                .credentialsProvider(credentialsProvider)
                                .clientConfiguration(clientBuilderConfiguration)
                                .region(ossConfig.getRegion())
                                .build();
                        OSS privateClient = publicClient;
                        // 非正式环境无法创建内网OSS实例
                        if ("prod".equals(profiles)) {
                            privateClient = OSSClientBuilder.create()
                                    .endpoint(ossConfig.getPrivateEndpoint())
                                    .credentialsProvider(credentialsProvider)
                                    .clientConfiguration(clientBuilderConfiguration)
                                    .region(ossConfig.getRegion())
                                    .build();
                        }
                        Assert.notNull(publicClient, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败. (阿里云公网对象存储服务配置错误)");
                        Assert.notNull(privateClient, "Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败. (阿里云私网对象存储服务配置错误)");
                        strixOssStore.addInstance(ossConfig.getKey(), new AliyunOssClient(publicClient, privateClient));
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
                log.error("Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 失败. (其他错误)", e);
            }
            if (success) {
                log.info("Strix OSS: 初始化对象存储服务实例 <" + ossConfig.getKey() + "> 完成.");
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
