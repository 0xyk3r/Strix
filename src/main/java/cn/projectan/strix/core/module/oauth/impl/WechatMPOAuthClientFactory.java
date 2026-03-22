package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.core.module.oauth.OAuthClientFactory;
import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.model.db.system.OauthConfig;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.WechatMPOAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 微信小程序 OAuth 客户端工厂
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class WechatMPOAuthClientFactory implements OAuthClientFactory {

    private final ObjectMapper objectMapper;
    @Qualifier("strixOAuthScheduler")
    private final ScheduledExecutorService scheduler;

    @Override
    public short supportedPlatform() {
        return OAuthPlatform.WECHAT_MP;
    }

    @Override
    public StrixOAuthClient<?> createClient(OauthConfig config) throws Exception {
        WechatMPOAuthConfig oauthConfig = objectMapper.readValue(config.getConfigData(), WechatMPOAuthConfig.class);
        oauthConfig.setId(config.getId());
        oauthConfig.setKey(config.getKey());
        oauthConfig.setName(config.getName());
        oauthConfig.setPlatform(config.getPlatform());
        return new WechatMPOAuthClient(oauthConfig, scheduler);
    }

}
