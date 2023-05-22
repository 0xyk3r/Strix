package cn.projectan.strix.service.impl;

import cn.projectan.strix.config.StrixSmsConfig;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.sms.AliyunSmsClient;
import cn.projectan.strix.mapper.SmsConfigMapper;
import cn.projectan.strix.model.constant.StrixSmsPlatform;
import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.service.SmsConfigService;
import cn.projectan.strix.task.StrixSmsTask;
import cn.projectan.strix.utils.SpringUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
@Slf4j
@Service
public class SmsConfigServiceImpl extends ServiceImpl<SmsConfigMapper, SmsConfig> implements SmsConfigService {

    @Override
    public void createSmsInstance(List<SmsConfig> smsConfigList) {
        StrixSmsTask strixSmsTask = SpringUtil.getBean(StrixSmsTask.class);
        StrixSmsConfig strixSmsConfig = SpringUtil.getBean(StrixSmsConfig.class);

        for (SmsConfig smsConfig : smsConfigList) {
            try {
                switch (smsConfig.getPlatform()) {
                    case StrixSmsPlatform.ALIYUN -> {
                        DefaultProfile profile = DefaultProfile.getProfile(smsConfig.getRegionId(), smsConfig.getAccessKey(), smsConfig.getAccessSecret());
                        Assert.notNull(profile, "Strix Sms: 初始化短信服务实例<" + smsConfig.getKey() + ">失败. (阿里云短信服务配置错误)");
                        strixSmsConfig.addInstance(smsConfig.getKey(), new AliyunSmsClient(new DefaultAcsClient(profile)));
                    }
                    case StrixSmsPlatform.TENCENT ->
                            throw new StrixException("Strix Sms: 初始化短信服务实例<" + smsConfig.getKey() + ">失败. (暂不支持腾讯云短信服务)");
                    default ->
                            throw new StrixException("Strix Sms: 初始化短信服务实例<" + smsConfig.getKey() + ">失败. (暂不支持该短信服务平台)");
                }
            } catch (Exception e) {
                log.error("Strix Sms: 初始化短信服务实例<" + smsConfig.getKey() + ">失败. (其他错误)", e);
            }
            log.info("Strix Sms: 初始化短信服务实例<" + smsConfig.getKey() + ">成功.");
        }

        // 全部初始化完成后，进行初始化签名和模板信息
        strixSmsTask.refreshSignList();
        strixSmsTask.refreshTemplateList();
    }

}
