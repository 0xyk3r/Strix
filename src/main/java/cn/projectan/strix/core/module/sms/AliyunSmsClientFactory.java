package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.dict.system.SmsPlatform;
import cn.projectan.strix.util.common.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 阿里云短信客户端工厂
 *
 * @author ProjectAn
 */
@Component
public class AliyunSmsClientFactory implements SmsClientFactory {

    @Override
    public short supportedPlatform() {
        return SmsPlatform.ALIYUN;
    }

    @Override
    public StrixSmsClient createClient(SmsConfig config) throws Exception {
        com.aliyun.teaopenapi.models.Config sdkConfig = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(config.getAccessKey())
                .setAccessKeySecret(config.getAccessSecret())
                .setRegionId(config.getRegionId());
        com.aliyun.dysmsapi20170525.Client client = new com.aliyun.dysmsapi20170525.Client(sdkConfig);
        Assert.notNull(client, I18nUtil.initFailed("field.aliyunSmsClient"));
        return new AliyunSmsClient(client);
    }

}
