package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oss.AliyunOssClient;
import cn.projectan.strix.core.module.oss.StrixOssConfig;
import cn.projectan.strix.mapper.OssConfigMapper;
import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.dict.StrixOssPlatform;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.task.StrixOssTask;
import cn.projectan.strix.utils.SpringUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * OSS 配置 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
@Slf4j
@Service
public class OssConfigServiceImpl extends ServiceImpl<OssConfigMapper, OssConfig> implements OssConfigService {

    @Value("${spring.profiles.active}")
    private String profiles;

    @Override
    public void createInstance(List<OssConfig> ossConfigList) {
        StrixOssTask strixOssTask = SpringUtil.getBean(StrixOssTask.class);
        StrixOssConfig strixOssConfig = SpringUtil.getBean(StrixOssConfig.class);

        for (OssConfig ossConfig : ossConfigList) {
            boolean success = true;
            try {
                switch (ossConfig.getPlatform()) {
                    case StrixOssPlatform.ALIYUN -> {
                        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
                        conf.setSupportCname(true);
                        OSS publicClient = new OSSClientBuilder().build(ossConfig.getPublicEndpoint(), ossConfig.getAccessKey(), ossConfig.getAccessSecret(), conf);
                        OSS privateClient = publicClient;
                        // 非正式环境无法创建内网OSS实例
                        if ("prod".equals(profiles)) {
                            privateClient = new OSSClientBuilder().build(ossConfig.getPrivateEndpoint(), ossConfig.getAccessKey(), ossConfig.getAccessSecret(), conf);
                        }
                        Assert.notNull(publicClient, "Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">失败. (阿里云公网对象存储服务配置错误)");
                        Assert.notNull(privateClient, "Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">失败. (阿里云私网对象存储服务配置错误)");
                        strixOssConfig.addInstance(ossConfig.getKey(), new AliyunOssClient(publicClient, privateClient));
                    }
                    case StrixOssPlatform.TENCENT ->
                            throw new StrixException("Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">失败. (暂不支持腾讯云对象存储服务)");
                    default ->
                            throw new StrixException("Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">失败. (暂不支持该对象存储服务平台)");
                }
            } catch (Exception e) {
                success = false;
                log.error("Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">失败. (其他错误)", e);
            }
            if (success) {
                log.info("Strix OSS: 初始化对象存储服务实例<" + ossConfig.getKey() + ">成功.");
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
