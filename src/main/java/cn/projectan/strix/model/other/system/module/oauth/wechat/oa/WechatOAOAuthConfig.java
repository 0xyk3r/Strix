package cn.projectan.strix.model.other.system.module.oauth.wechat.oa;

import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthConfig;
import lombok.Data;

/**
 * 微信公众平台 OAuth 配置
 *
 * @author ProjectAn
 * @since 2024/4/3 17:32
 */
@Data
public class WechatOAOAuthConfig extends BaseOAuthConfig {

    /**
     * 微信公众号开发者ID (AppID)
     */
    private String appId;

    /**
     * 微信公众号开发者密码 (AppSecret)
     */
    private String appSecret;

    /**
     * 微信公众号令牌 (Token)
     */
    private String token;

    /**
     * 微信公众号首页地址
     */
    private String webIndexUrl;

    /**
     * 微信公众号授权回调地址
     */
    private String authUrl;

}
