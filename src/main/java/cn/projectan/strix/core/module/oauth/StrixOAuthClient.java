package cn.projectan.strix.core.module.oauth;

import cn.projectan.strix.model.db.OauthPush;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthConfig;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;

import java.util.Map;

/**
 * Strix OAuth 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 16:39
 */
public abstract class StrixOAuthClient {

    /**
     * 获取配置id
     *
     * @return 配置id
     */
    public abstract String getConfigId();

    /**
     * 获取配置名称
     *
     * @return 配置名称
     */
    public abstract String getConfigName();

    /**
     * 获取平台
     *
     * @return 平台
     */
    public abstract int getPlatform();

    /**
     * 是否支持推送服务
     */
    public abstract boolean supportPush();

    /**
     * 获取配置
     *
     * @return 配置
     */
    public abstract BaseOAuthConfig getConfig();

    /**
     * 使用 code 换取 accessToken
     *
     * @param code code
     * @return accessToken
     */
    public abstract BaseOAuthUserInfo grantBaseUserInfo(String code);

    /**
     * 使用 用户accessToken 换取用户信息
     *
     * @param accessToken 用户accessToken
     * @return 用户信息
     */
    public abstract Map<String, String> grantMoreUserInfo(String accessToken);

    public abstract void generatePush(String openId, String content);

    public abstract void push(OauthPush oauthPush);

}
