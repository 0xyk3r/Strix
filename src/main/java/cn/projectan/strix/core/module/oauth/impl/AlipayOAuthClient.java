package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.core.exception.StrixOAuthException;
import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.model.other.system.module.oauth.AlipayOAuthConfig;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 16:41
 */
@Slf4j
public class AlipayOAuthClient extends StrixOAuthClient<AlipayOAuthConfig> {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    protected final DefaultAlipayClient client;

    public AlipayOAuthClient(AlipayOAuthConfig config) {
        super(config);
        Assert.notNull(config, "Strix OAuth: 初始化支付宝 OAuth 服务实例失败. (配置信息为空)");
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
            throw new StrixOAuthException("Strix OAuth: 初始化支付宝 OAuth 服务实例失败. (配置信息错误)", e);
        }
    }

    @Override
    public BaseOAuthUserInfo auth(String code) {
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode(code);
        request.setGrantType(GRANT_TYPE_AUTHORIZATION_CODE);

        try {
            AlipaySystemOauthTokenResponse response = client.certificateExecute(request);
            if (!response.isSuccess()) {
                String errorMsg = String.format("Strix OAuth: 获取支付宝 OAuth Token 失败. (code: %s, subCode: %s, subMsg: %s)",
                        response.getCode(), response.getSubCode(), response.getSubMsg());
                throw new StrixOAuthException(errorMsg);
            }

            log.debug("Strix OAuth: 获取支付宝 OAuth Token 成功. Response: {}", response.getBody());

            BaseOAuthUserInfo oAuthUserInfo = new BaseOAuthUserInfo();
            oAuthUserInfo.setConfigId(config.getId());
            oAuthUserInfo.setAppId(config.getAppId());
            oAuthUserInfo.setOpenId(response.getOpenId());
            Assert.hasText(oAuthUserInfo.getOpenId(), "Strix OAuth: 获取支付宝 OAuth 授权凭证失败, OpenId 为空.");
            oAuthUserInfo.setAccessToken(response.getAccessToken());
            oAuthUserInfo.setRefreshToken(response.getRefreshToken());
            oAuthUserInfo.setExpiresIn(Integer.parseInt(response.getExpiresIn()));
            oAuthUserInfo.setUnionId(response.getUnionId());
            return oAuthUserInfo;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取支付宝 OAuth Token 失败. (code: {})", code, e);
            throw new StrixOAuthException("Strix OAuth: 获取支付宝 OAuth Token 失败.", e);
        }
    }

    @Override
    public Map<String, String> getUserInfo(String accessToken) {
        AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
        try {
            AlipayUserInfoShareResponse response = client.certificateExecute(request, accessToken);
            if (!response.isSuccess()) {
                String errorMsg = String.format("Strix OAuth: 获取支付宝 OAuth 用户信息失败. (subCode: %s, subMsg: %s)",
                        response.getSubCode(), response.getSubMsg());
                throw new StrixOAuthException(errorMsg);
            }

            log.debug("Strix OAuth: 获取支付宝 OAuth 用户信息成功. Response: {}", response.getBody());

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userId", response.getUserId());
            userInfo.put("nickName", response.getNickName());
            userInfo.put("avatar", response.getAvatar());
            userInfo.put("province", response.getProvince());
            userInfo.put("city", response.getCity());
            userInfo.put("gender", response.getGender());
            return userInfo;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取支付宝 OAuth 用户信息失败. (accessToken: {})", accessToken, e);
            throw new StrixOAuthException("Strix OAuth: 获取支付宝 OAuth 用户信息失败.", e);
        }
    }


}
