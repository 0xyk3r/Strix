package cn.projectan.strix.initialize;

import cn.projectan.strix.config.GlobalWechatConfig;
import cn.projectan.strix.model.db.WechatConfig;
import cn.projectan.strix.service.WechatConfigService;
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
 * @date 2021/8/24 18:51
 */
@Slf4j
@Order(value = 10)
@Component
@ConditionalOnProperty(prefix = "strix", name = "use-wechat-auth", havingValue = "true")
public class WechatConfigInit implements ApplicationRunner {

    @Autowired
    private WechatConfigService wechatConfigService;
    @Autowired
    private GlobalWechatConfig globalWechatConfig;

    @Override
    public void run(ApplicationArguments args) {
        List<WechatConfig> wechatConfigList = wechatConfigService.list();

        for (WechatConfig wechatConfig : wechatConfigList) {
            globalWechatConfig.addInstance(wechatConfig);
        }
    }
}
