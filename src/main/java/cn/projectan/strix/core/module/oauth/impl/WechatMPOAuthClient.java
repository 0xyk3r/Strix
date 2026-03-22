package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.WechatMPOAuthConfig;
import cn.projectan.strix.util.module.oauth.WechatMPOAuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 微信小程序 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2026/1/29 09:42
 */
@Slf4j
public class WechatMPOAuthClient extends AbstractWechatOAuthClient<WechatMPOAuthConfig> {

    public WechatMPOAuthClient(WechatMPOAuthConfig config, ScheduledExecutorService scheduler) {
        super(config, scheduler);
        Assert.notNull(config, "Strix OAuth: 初始化微信小程序 OAuth 服务实例失败. (配置信息为空)");
        // 初始化 AccessToken 刷新任务
        initAccessTokenRefreshTask(config.getAppId(), config.getAppSecret());
    }

    @Override
    public BaseOAuthUserInfo auth(String code) {
        return WechatMPOAuthUtil.auth(config, code);
    }

    @Override
    public String getPhoneNumber(String code) {
        return WechatMPOAuthUtil.getPhoneNumber(getAccessToken(), code);
    }


}
