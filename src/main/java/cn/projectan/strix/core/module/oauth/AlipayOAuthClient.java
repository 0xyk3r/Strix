package cn.projectan.strix.core.module.oauth;

import cn.projectan.strix.model.db.OauthPush;
import cn.projectan.strix.model.other.module.oauth.AlipayOAuthConfig;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthConfig;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 支付宝 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 16:41
 */
@Slf4j
public class AlipayOAuthClient extends StrixOAuthClient {

    protected final AlipayOAuthConfig config;
    protected final DefaultAlipayClient client;

    public AlipayOAuthClient(AlipayOAuthConfig config) {
        super();
        Assert.notNull(config, "Strix OAuth: 初始化支付宝 OAuth 服务实例失败. (配置信息为空)");
        this.config = config;
        try {
            CertAlipayRequest certAlipayRequest = new CertAlipayRequest();
            certAlipayRequest.setServerUrl(config.getServerUrl());
            certAlipayRequest.setAppId(config.getAppId());
            certAlipayRequest.setPrivateKey(config.getPrivateKey());
            certAlipayRequest.setFormat(config.getFormat());
            certAlipayRequest.setCharset(config.getCharset());
            certAlipayRequest.setSignType(config.getSignType());
            certAlipayRequest.setCertPath(config.getAppCertPath());
            certAlipayRequest.setAlipayPublicCertPath(config.getAlipayCertPath());
            certAlipayRequest.setRootCertPath(config.getAlipayRootCertPath());
            client = new DefaultAlipayClient(certAlipayRequest);
        } catch (Exception e) {
            throw new RuntimeException("Strix OAuth: 初始化支付宝 OAuth 服务实例失败. (配置信息错误)", e);
        }
    }

    @Override
    public String getConfigId() {
        return config.getId();
    }

    @Override
    public String getConfigName() {
        return config.getName();
    }

    @Override
    public int getPlatform() {
        return config.getPlatform();
    }

    @Override
    public boolean supportPush() {
        return false;
    }

    @Override
    public BaseOAuthConfig getConfig() {
        return config;
    }

    @Override
    public BaseOAuthUserInfo grantBaseUserInfo(String code) {
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode(code);
        request.setGrantType("authorization_code");
        try {
            AlipaySystemOauthTokenResponse response = client.certificateExecute(request);
            Assert.isTrue(response.isSuccess(), "Strix OAuth: 获取支付宝 OAuth Token 失败. (response: " + response.getBody() + ")");
            log.info(response.toString());

            BaseOAuthUserInfo oAuthUserInfo = new BaseOAuthUserInfo();
            oAuthUserInfo.setConfigId(config.getId());
            oAuthUserInfo.setAppId(config.getAppId());
            oAuthUserInfo.setOpenId(response.getOpenId());
            Assert.hasText(oAuthUserInfo.getOpenId(), "Strix OAuth: 获取微信 OAuth 授权凭证失败, OpenId 为空.");
            oAuthUserInfo.setAccessToken(response.getAccessToken());
            oAuthUserInfo.setRefreshToken(response.getRefreshToken());
            oAuthUserInfo.setExpiresIn(Integer.parseInt(response.getExpiresIn()));
            oAuthUserInfo.setUnionId(response.getUnionId());
            return oAuthUserInfo;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取支付宝 OAuth Token 失败. (code: {})", code, e);
        }
        return null;
    }

    @Override
    public Map<String, String> grantMoreUserInfo(String accessToken) {
        AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
        try {
            AlipayUserInfoShareResponse response = client.certificateExecute(request, accessToken);
            Assert.isTrue(response.isSuccess(), "Strix OAuth: 获取支付宝 OAuth 用户信息失败. (response: " + response.getBody() + ")");
            log.info(response.toString());
        } catch (Exception e) {
            log.error("Strix OAuth: 获取支付宝 OAuth 用户信息失败. (accessToken: {})", accessToken, e);
        }
        return null;
    }

    @Override
    public void generatePush(String openId, String content) {
        log.warn("Strix OAuth: 支付宝 OAuth 服务实例不支持推送服务.");
    }

    @Override
    public void push(OauthPush oauthPush) {
        log.warn("Strix OAuth: 支付宝 OAuth 服务实例不支持推送服务.");
    }

}
