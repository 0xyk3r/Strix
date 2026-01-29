package cn.projectan.strix.core.module.oauth;

import cn.projectan.strix.model.db.system.OauthPush;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthConfig;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import lombok.Getter;

import java.util.Map;

/**
 * Strix OAuth 客户端抽象基类
 *
 * @author ProjectAn
 * @since 2024/4/3 16:39
 */
public abstract class StrixOAuthClient<T extends BaseOAuthConfig> {

    @Getter
    protected final T config;

    protected StrixOAuthClient(T config) {
        this.config = config;
    }

    /**
     * 获取配置 ID
     *
     * @return 配置 ID
     */
    public String getConfigId() {
        return config.getId();
    }

    /**
     * 获取配置名称
     *
     * @return 配置名称
     */
    public String getConfigName() {
        return config.getName();
    }

    /**
     * 获取平台
     *
     * @return 平台
     */
    public short getPlatform() {
        return config.getPlatform();
    }

    /**
     * 使用 code 换取用户登录信息
     *
     * @param code code
     * @return OAuth用户信息，失败时抛出异常
     * @throws cn.projectan.strix.core.exception.StrixOAuthException OAuth异常
     */
    public abstract BaseOAuthUserInfo auth(String code);

    /**
     * 使用 用户accessToken 换取用户信息
     *
     * @param accessToken 用户accessToken
     * @return 用户信息，如果平台不支持返回空Map
     */
    public abstract Map<String, String> getUserInfo(String accessToken);

    /**
     * 使用 code 换取用户手机号
     *
     * @param code code
     * @return 手机号，如果平台不支持返回null
     */
    public abstract String getPhoneNumber(String code);

    /**
     * 是否支持推送服务
     *
     * @return true表示支持，false表示不支持
     */
    public abstract boolean supportPush();

    /**
     * 生成推送消息
     *
     * @param openId  用户OpenId
     * @param content 推送内容
     */
    public abstract void generatePush(String openId, String content);

    /**
     * 推送消息
     *
     * @param oauthPush 推送信息
     */
    public abstract void push(OauthPush oauthPush);

}
