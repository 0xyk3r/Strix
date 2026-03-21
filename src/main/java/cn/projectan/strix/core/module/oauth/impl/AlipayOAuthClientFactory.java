package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.core.module.oauth.OAuthClientFactory;
import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.model.db.system.OauthConfig;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.AlipayOAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 支付宝 OAuth 客户端工厂
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class AlipayOAuthClientFactory implements OAuthClientFactory {

    private final ObjectMapper objectMapper;

    @Override
    public short supportedPlatform() {
        return OAuthPlatform.ALIPAY;
    }

    @Override
    public StrixOAuthClient<?> createClient(OauthConfig config) throws Exception {
        AlipayOAuthConfig oauthConfig = objectMapper.readValue(config.getConfigData(), AlipayOAuthConfig.class);
        oauthConfig.setId(config.getId());
        oauthConfig.setKey(config.getKey());
        oauthConfig.setName(config.getName());
        oauthConfig.setPlatform(config.getPlatform());
        return new AlipayOAuthClient(oauthConfig);
    }

}
