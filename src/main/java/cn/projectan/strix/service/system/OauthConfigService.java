package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.core.module.oauth.impl.AlipayOAuthClient;
import cn.projectan.strix.core.module.oauth.impl.WechatMPOAuthClient;
import cn.projectan.strix.core.module.oauth.impl.WechatOAOAuthClient;
import cn.projectan.strix.mapper.system.OauthConfigMapper;
import cn.projectan.strix.model.db.system.OauthConfig;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.AlipayOAuthConfig;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.WechatMPOAuthConfig;
import cn.projectan.strix.model.other.system.module.oauth.wechat.oa.WechatOAOAuthConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Strix OAuth 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OauthConfigService extends ServiceImpl<OauthConfigMapper, OauthConfig> {

    private final StrixOAuthStore strixOAuthStore;
    private final ObjectMapper objectMapper;

    /**
     * 创建 OAuth 配置
     *
     * @param oauthConfigList OAuth 配置列表
     */
    public void createInstance(List<OauthConfig> oauthConfigList) {
        for (OauthConfig oauthConfig : oauthConfigList) {
            try {
                switch (oauthConfig.getPlatform()) {
                    case OAuthPlatform.WECHAT_OA -> {
                        WechatOAOAuthConfig wechatOAOAuthConfig = objectMapper.readValue(oauthConfig.getConfigData(), WechatOAOAuthConfig.class);
                        wechatOAOAuthConfig.setId(oauthConfig.getId());
                        wechatOAOAuthConfig.setKey(oauthConfig.getKey());
                        wechatOAOAuthConfig.setName(oauthConfig.getName());
                        wechatOAOAuthConfig.setPlatform(oauthConfig.getPlatform());
                        strixOAuthStore.addInstance(oauthConfig.getKey(), new WechatOAOAuthClient(wechatOAOAuthConfig));
                        log.info("Strix OAuth: 初始化 <微信公众号> 服务实例 <{}> 成功.", oauthConfig.getName());
                    }
                    case OAuthPlatform.WECHAT_MP -> {
                        WechatMPOAuthConfig wechatMPOAuthConfig = objectMapper.readValue(oauthConfig.getConfigData(), WechatMPOAuthConfig.class);
                        wechatMPOAuthConfig.setId(oauthConfig.getId());
                        wechatMPOAuthConfig.setKey(oauthConfig.getKey());
                        wechatMPOAuthConfig.setName(oauthConfig.getName());
                        wechatMPOAuthConfig.setPlatform(oauthConfig.getPlatform());
                        strixOAuthStore.addInstance(oauthConfig.getKey(), new WechatMPOAuthClient(wechatMPOAuthConfig));
                        log.info("Strix OAuth: 初始化 <微信小程序> 服务实例 <{}> 成功.", oauthConfig.getName());
                    }
                    case OAuthPlatform.ALIPAY -> {
                        AlipayOAuthConfig alipayOAuthConfig = objectMapper.readValue(oauthConfig.getConfigData(), AlipayOAuthConfig.class);
                        alipayOAuthConfig.setId(oauthConfig.getId());
                        alipayOAuthConfig.setKey(oauthConfig.getKey());
                        alipayOAuthConfig.setName(oauthConfig.getName());
                        alipayOAuthConfig.setPlatform(oauthConfig.getPlatform());
                        strixOAuthStore.addInstance(oauthConfig.getKey(), new AlipayOAuthClient(alipayOAuthConfig));
                        log.info("Strix OAuth: 初始化 <支付宝> 服务实例 <{}> 成功.", oauthConfig.getName());
                    }
                }
            } catch (Exception e) {
                log.error("Strix OAuth: 初始化 OAuth 服务实例 <{}> 失败. (配置信息错误)", oauthConfig.getName(), e);
            }
        }
    }

}
