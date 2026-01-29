package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.model.db.system.OauthPush;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.WechatMPOAuthConfig;
import cn.projectan.strix.util.module.oauth.WechatMPOAuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 微信小程序 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2026/1/29 09:42
 */
@Slf4j
public class WechatMPOAuthClient extends AbstractWechatOAuthClient<WechatMPOAuthConfig> {

    public WechatMPOAuthClient(WechatMPOAuthConfig config) {
        super(config);
        Assert.notNull(config, "Strix OAuth: 初始化微信小程序 OAuth 服务实例失败. (配置信息为空)");
        // 初始化 AccessToken 刷新任务
        initAccessTokenRefreshTask(config.getAppId(), config.getAppSecret());
    }

    @Override
    public BaseOAuthUserInfo auth(String code) {
        return WechatMPOAuthUtil.auth(config, code);
    }

    @Override
    public Map<String, String> getUserInfo(String accessToken) {
        throw new UnsupportedOperationException("Strix OAuth: 微信小程序 OAuth 服务实例不支持获取用户信息.");
    }

    @Override
    public String getPhoneNumber(String code) {
        return WechatMPOAuthUtil.getPhoneNumber(getAccessToken(), code);
    }

    @Override
    public boolean supportPush() {
        return false;
    }

    @Override
    public void generatePush(String openId, String content) {
        throw new UnsupportedOperationException("Strix OAuth: 微信小程序 OAuth 服务实例不支持生成推送消息.");
    }

    @Override
    public void push(OauthPush oauthPush) {
        throw new UnsupportedOperationException("Strix OAuth: 微信小程序 OAuth 服务实例不支持推送消息.");
    }


}
