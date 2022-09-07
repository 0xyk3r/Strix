package cn.projectan.strix.initialize;

import cn.projectan.strix.config.AliyunSmsConfig;
import cn.projectan.strix.model.db.AliyunSms;
import cn.projectan.strix.service.AliyunSmsService;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/2 17:46
 */
@Slf4j
@Order(value = 1)
@Component
@ConditionalOnProperty(prefix = "strix", name = "use-sms-aliyun", havingValue = "true")
public class AliyunSmsInit implements ApplicationRunner {

    @Autowired
    private AliyunSmsService aliyunSmsService;

    @Autowired
    private AliyunSmsConfig aliyunSmsConfig;

    @Override
    public void run(ApplicationArguments args) {
        List<AliyunSms> aliyunSmsList = aliyunSmsService.list();

        for (AliyunSms aliyunSms : aliyunSmsList) {
            log.info("ProjectAn Strix 正在创建阿里云短信服务实例: " + aliyunSms.getId());
            DefaultProfile profile = DefaultProfile.getProfile(aliyunSms.getRegionId(), aliyunSms.getAccessKey(), aliyunSms.getAccessSecret());
            aliyunSmsConfig.addInstance(aliyunSms.getId(), new DefaultAcsClient(profile));
        }
    }
}
