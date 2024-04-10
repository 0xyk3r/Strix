package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.module.oauth.AlipayOAuthClient;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.core.module.oauth.WechatOAuthClient;
import cn.projectan.strix.mapper.OauthConfigMapper;
import cn.projectan.strix.model.db.OauthConfig;
import cn.projectan.strix.model.dict.OAuthPlatform;
import cn.projectan.strix.model.other.module.oauth.AlipayOAuthConfig;
import cn.projectan.strix.model.other.module.oauth.WechatOAuthConfig;
import cn.projectan.strix.service.OauthConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Strix OAuth 配置 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OauthConfigServiceImpl extends ServiceImpl<OauthConfigMapper, OauthConfig> implements OauthConfigService {

    private final StrixOAuthStore strixOAuthStore;
    private final ObjectMapper objectMapper;

    @Override
    public void createInstance(List<OauthConfig> oauthConfigList) {
        for (OauthConfig oauthConfig : oauthConfigList) {
            try {
                switch (oauthConfig.getPlatform()) {
                    case OAuthPlatform.WECHAT -> {
                        WechatOAuthConfig wechatOAuthConfig = objectMapper.readValue(oauthConfig.getConfigData(), WechatOAuthConfig.class);
                        wechatOAuthConfig.setId(oauthConfig.getId());
                        wechatOAuthConfig.setName(oauthConfig.getName());
                        wechatOAuthConfig.setPlatform(oauthConfig.getPlatform());
                        strixOAuthStore.addInstance(oauthConfig.getId(), new WechatOAuthClient(wechatOAuthConfig));
                        log.info("Strix OAuth: 初始化 OAuth 服务实例 <" + oauthConfig.getName() + "> 成功.");
                    }
                    case OAuthPlatform.ALIPAY -> {
                        AlipayOAuthConfig alipayOAuthConfig = objectMapper.readValue(oauthConfig.getConfigData(), AlipayOAuthConfig.class);
                        alipayOAuthConfig.setId(oauthConfig.getId());
                        alipayOAuthConfig.setName(oauthConfig.getName());
                        alipayOAuthConfig.setPlatform(oauthConfig.getPlatform());
                        strixOAuthStore.addInstance(oauthConfig.getId(), new AlipayOAuthClient(alipayOAuthConfig));
                        log.info("Strix OAuth: 初始化 OAuth 服务实例 <" + oauthConfig.getName() + "> 成功.");
                    }
                }
            } catch (Exception e) {
                log.error("Strix OAuth: 初始化 OAuth 服务实例 <" + oauthConfig.getName() + "> 失败. (配置信息错误)", e);
            }
        }
    }

}
