package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.core.module.oauth.OAuthClientFactory;
import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.model.db.system.OauthConfig;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.wechat.oa.WechatOAOAuthConfig;
import cn.projectan.strix.service.system.OauthPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 微信公众号 OAuth 客户端工厂
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class WechatOAOAuthClientFactory implements OAuthClientFactory {

    private final ObjectMapper objectMapper;
    private final OauthPushService oauthPushService;
    @Qualifier("strixOAuthScheduler")
    private final ScheduledExecutorService scheduler;

    @Override
    public short supportedPlatform() {
        return OAuthPlatform.WECHAT_OA;
    }

    @Override
    public StrixOAuthClient<?> createClient(OauthConfig config) throws Exception {
        WechatOAOAuthConfig oauthConfig = objectMapper.readValue(config.getConfigData(), WechatOAOAuthConfig.class);
        oauthConfig.setId(config.getId());
        oauthConfig.setKey(config.getKey());
        oauthConfig.setName(config.getName());
        oauthConfig.setPlatform(config.getPlatform());
        return new WechatOAOAuthClient(oauthConfig, scheduler, objectMapper, oauthPushService);
    }

}
