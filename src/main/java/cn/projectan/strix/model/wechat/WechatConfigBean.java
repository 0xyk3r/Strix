package cn.projectan.strix.model.wechat;

import lombok.Data;

/**
 * @author ProjectAn
 * @date 2021/8/24 19:08
 */
@Data
public class WechatConfigBean {

    private String id;
    /**
     * 微信公众号名称
     */
    private String name;

    /**
     * 微信公众号开发者ID(AppID)
     */
    private String appId;

    /**
     * 微信公众号开发者密码(AppSecret)
     */
    private String appSecret;

    /**
     * 微信公众号令牌(Token)
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

    private String accessToken;

    private String jsApiTicket;

}
