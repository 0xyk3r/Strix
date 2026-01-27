package cn.projectan.strix.service.system;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.sms.AliyunSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.mapper.system.SmsConfigMapper;
import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.dict.system.StrixSmsPlatform;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.task.system.StrixSmsTask;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * Strix SMS 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "strix.module", name = "sms", havingValue = "true")
public class SmsConfigService extends ServiceImpl<SmsConfigMapper, SmsConfig> {

    /**
     * 创建实例
     *
     * @param smsConfigList 短信配置列表
     */
    public void createInstance(List<SmsConfig> smsConfigList) {
        StrixSmsTask strixSmsTask = SpringUtil.getBean(StrixSmsTask.class);
        StrixSmsStore strixSmsStore = SpringUtil.getBean(StrixSmsStore.class);

        for (SmsConfig smsConfig : smsConfigList) {
            boolean success = true;
            try {
                switch (smsConfig.getPlatform()) {
                    case StrixSmsPlatform.ALIYUN -> {
                        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                                .setAccessKeyId(smsConfig.getAccessKey())
                                .setAccessKeySecret(smsConfig.getAccessSecret())
                                .setRegionId(smsConfig.getRegionId());
                        com.aliyun.dysmsapi20170525.Client client = new com.aliyun.dysmsapi20170525.Client(config);
                        Assert.notNull(client, "Strix SMS: 初始化短信服务实例 <" + smsConfig.getKey() + "> 失败.");
                        strixSmsStore.addInstance(smsConfig.getKey(), new AliyunSmsClient(client));
                    }
                    case StrixSmsPlatform.TENCENT ->
                            throw new StrixException("Strix SMS: 初始化短信服务实例 <" + smsConfig.getKey() + "> 失败. (暂不支持腾讯云短信服务)");
                    default ->
                            throw new StrixException("Strix SMS: 初始化短信服务实例 <" + smsConfig.getKey() + "> 失败. (暂不支持该短信服务平台)");
                }
            } catch (Exception e) {
                success = false;
                log.error("Strix SMS: 初始化短信服务实例 <{}> 失败.", smsConfig.getKey(), e);
            }
            if (success) {
                log.info("Strix SMS: 初始化短信服务实例 <{}> 成功.", smsConfig.getKey());
            }
        }

        // 全部初始化完成后，进行初始化签名和模板信息
        strixSmsTask.refreshSignList();
        strixSmsTask.refreshTemplateList();
    }

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    public CommonSelectDataResp getSelectData() {
        List<SmsConfig> smsConfigList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(smsConfigList, "key", "key", "name");
    }

}
