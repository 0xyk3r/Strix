package cn.projectan.strix.model.other.system.module.oauth.wechat.mp;

import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthConfig;
import lombok.Data;

/**
 * 微信小程序 OAuth 配置
 *
 * @author ProjectAn
 * @since 2026/1/29 09:39
 */
@Data
public class WechatMPOAuthConfig extends BaseOAuthConfig {

    /**
     * 微信小程序开发者ID (AppID)
     */
    private String appId;

    /**
     * 微信小程序开发者密码 (AppSecret)
     */
    private String appSecret;

}
