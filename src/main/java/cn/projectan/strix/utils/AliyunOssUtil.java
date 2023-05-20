package cn.projectan.strix.utils;

import cn.projectan.strix.model.db.AliyunOss;
import cn.projectan.strix.model.system.AliyunOssInstance;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2021/5/2 17:22
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class AliyunOssUtil {

    @Value("${spring.profiles.active}")
    private String profiles;

    public AliyunOssInstance createInstance(AliyunOss aliyunOss) {
        if (aliyunOss == null) {
            return null;
        }
        AliyunOssInstance aliyunOssInstance = new AliyunOssInstance();
        try {
            aliyunOssInstance.setPublicInstance(createPublicInstance(aliyunOss));
            // 开发环境无法创建内网OSS实例
            if ("dev".equals(profiles)) {
                aliyunOssInstance.setPrivateInstance(aliyunOssInstance.getPublicInstance());
            } else {
                aliyunOssInstance.setPrivateInstance(createPrivateInstance(aliyunOss));
            }
        } catch (Exception e) {
            log.error("ProjectAn Strix 创建阿里云OSS实例时出错", e);
        }
        return aliyunOssInstance;
    }

    public OSS createPublicInstance(AliyunOss aliyunOss) {
        log.info("ProjectAn Strix 正在创建阿里云OSS实例: " + aliyunOss.getPublicEndpoint());
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSupportCname(true);
        return new OSSClientBuilder().build(aliyunOss.getPublicEndpoint(), aliyunOss.getAccessKeyId(), aliyunOss.getAccessKeySecret(), conf);
    }

    public OSS createPrivateInstance(AliyunOss aliyunOss) {
        if ("dev".equals(profiles)) {
            log.info("ProjectAn Strix 开发环境无法创建阿里云内网OSS实例: ");
            return null;
        }
        log.info("ProjectAn Strix 正在创建阿里云内网OSS实例: " + aliyunOss.getPrivateEndpoint());
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSupportCname(true);
        return new OSSClientBuilder().build(aliyunOss.getPrivateEndpoint(), aliyunOss.getAccessKeyId(), aliyunOss.getAccessKeySecret(), conf);
    }

}
