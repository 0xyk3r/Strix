package cn.projectan.strix.core.module.oauth;

import cn.projectan.strix.model.db.system.OauthConfig;

/**
 * OAuth 客户端工厂接口
 * <p>
 * 实现此接口并注册为 Spring Bean 即可自动支持新的 OAuth 平台，
 * 无需修改 OauthConfigService 代码。
 *
 * @author ProjectAn
 */
public interface OAuthClientFactory {

    /**
     * 该工厂支持的平台标识
     *
     * @return 平台标识，对应 {@link cn.projectan.strix.model.dict.system.OAuthPlatform} 中的常量
     */
    short supportedPlatform();

    /**
     * 根据配置创建 OAuth 客户端实例
     *
     * @param config OAuth 配置
     * @return OAuth 客户端实例
     * @throws Exception 创建失败时抛出异常
     */
    StrixOAuthClient<?> createClient(OauthConfig config) throws Exception;

}
