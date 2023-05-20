package cn.projectan.strix.initialize;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.sms.AliyunSmsClient;
import cn.projectan.strix.model.constant.StrixSmsPlatform;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.task.StrixSmsTask;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/2 17:46
 */
@Slf4j
@Order(10)
@Component
@ConditionalOnBean(StrixSmsConfig.class)
public class StrixSmsInit implements ApplicationRunner {

    @Autowired
    private SmsConfigService smsConfigService;

    @Autowired
    private StrixSmsConfig strixSmsConfig;
    @Autowired
    private StrixSmsTask strixSmsTask;

    @Override
    public void run(ApplicationArguments args) {
        List<SmsConfig> smsConfigList = smsConfigService.list();

        for (SmsConfig smsConfig : smsConfigList) {
            try {
                switch (smsConfig.getPlatform()) {
                    case StrixSmsPlatform.ALIYUN -> {
                        DefaultProfile profile = DefaultProfile.getProfile(smsConfig.getRegionId(), smsConfig.getAccessKey(), smsConfig.getAccessSecret());
                        Assert.notNull(profile, "Strix Sms: 初始化短信服务实例<" + smsConfig.getId() + ">失败. (阿里云短信服务配置错误)");
                        strixSmsConfig.addInstance(smsConfig.getId(), new AliyunSmsClient(new DefaultAcsClient(profile)));
                    }
                    case StrixSmsPlatform.TENCENT ->
                            throw new StrixException("Strix Sms: 初始化短信服务实例<" + smsConfig.getId() + ">失败. (暂不支持腾讯云短信服务)");
                    default ->
                            throw new StrixException("Strix Sms: 初始化短信服务实例<" + smsConfig.getId() + ">失败. (暂不支持该短信服务平台)");
                }
            } catch (Exception e) {
                log.error("Strix Sms: 初始化短信服务实例<" + smsConfig.getId() + ">失败. (其他错误)", e);
            }
            log.info("Strix Sms: 初始化短信服务实例<" + smsConfig.getId() + ">成功.");
        }

        // 全部初始化完成后，进行初始化签名和模板信息
        strixSmsTask.refreshSignList();
        strixSmsTask.refreshTemplateList();
    }
}
