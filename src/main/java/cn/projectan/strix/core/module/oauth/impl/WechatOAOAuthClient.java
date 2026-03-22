package cn.projectan.strix.core.module.oauth.impl;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.exception.StrixOAuthException;
import cn.projectan.strix.model.db.system.OauthPush;
import cn.projectan.strix.model.dict.system.OAuthPushStatus;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.system.module.oauth.wechat.oa.WechatOAOAuthConfig;
import cn.projectan.strix.service.system.OauthPushService;
import cn.projectan.strix.util.common.SpringUtil;
import cn.projectan.strix.util.http.OkHttpUtil;
import cn.projectan.strix.util.module.oauth.WechatOAOAuthUtil;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.Assert;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信公众号 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 17:34
 */
@Slf4j
public class WechatOAOAuthClient extends AbstractWechatOAuthClient<WechatOAOAuthConfig> {

    private static final String AUTH_URL_TEMPLATE = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    private static final String PUSH_URL_TEMPLATE = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    private static final String AUTHORIZE_URL_TEMPLATE = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=ProjectAn#wechat_redirect";

    private final ObjectMapper objectMapper;
    private final OauthPushService oauthPushService;

    @Getter
    @Setter
    protected String jsApiTicket;

    public WechatOAOAuthClient(WechatOAOAuthConfig config) {
        super(config);
        Assert.notNull(config, "Strix OAuth: 初始化微信公众号 OAuth 服务实例失败. (配置信息为空)");
        this.objectMapper = SpringUtil.getBean(ObjectMapper.class);
        this.oauthPushService = SpringUtil.getBean(OauthPushService.class);
        // 初始化 AccessToken 和 JsApiTicket 刷新任务
        initAccessTokenRefreshTask(config.getAppId(), config.getAppSecret());
    }

    @Override
    protected void onAccessTokenRefreshed(String newAccessToken) {
        // AccessToken 刷新后，同时刷新 JsApiTicket
        try {
            String newJsApiTicket = WechatOAOAuthUtil.getJsApiTicket(newAccessToken);
            setJsApiTicket(newJsApiTicket);
            log.info("Strix OAuth: 刷新微信公众号实例 <{}> 的 JsApiTicket 完成.", config.getName());
        } catch (Exception e) {
            log.error("Strix OAuth: 刷新微信公众号实例 <{}> 的 JsApiTicket 失败.", config.getName(), e);
        }
    }

    @Override
    public BaseOAuthUserInfo auth(String code) {
        String requestUrl = String.format(AUTH_URL_TEMPLATE, config.getAppId(), config.getAppSecret(), code);
        try {
            String responseStr = OkHttpUtil.get(requestUrl);
            Assert.hasText(responseStr, "Strix OAuth: 获取微信公众号 OAuth 授权凭证失败.");

            Map<String, Object> data = objectMapper.readValue(responseStr, new TypeReference<>() {
            });
            Assert.notNull(data, "Strix OAuth: 获取微信公众号 OAuth 授权凭证失败.");

            // 检查是否有错误码
            Integer errorCode = MapUtil.getInt(data, "errcode");
            if (errorCode != null && errorCode != 0) {
                String errorMsg = MapUtil.getStr(data, "errmsg");
                String msg = String.format("Strix OAuth: 获取微信公众号 OAuth 授权凭证失败, errcode:%d errmsg:%s", errorCode, errorMsg);
                throw new StrixOAuthException(msg);
            }

            BaseOAuthUserInfo oAuthUserInfo = new BaseOAuthUserInfo();
            oAuthUserInfo.setConfigId(config.getId());
            oAuthUserInfo.setAppId(config.getAppId());
            oAuthUserInfo.setOpenId(MapUtil.getStr(data, "openid"));
            Assert.hasText(oAuthUserInfo.getOpenId(), "Strix OAuth: 获取微信公众号 OAuth 授权凭证失败, OpenId 为空.");
            oAuthUserInfo.setAccessToken(MapUtil.getStr(data, "access_token"));
            oAuthUserInfo.setRefreshToken(MapUtil.getStr(data, "refresh_token"));
            oAuthUserInfo.setExpiresIn(MapUtil.getInt(data, "expires_in"));
            oAuthUserInfo.setUnionId(MapUtil.getStr(data, "unionid"));
            return oAuthUserInfo;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信公众号 OAuth 授权凭证失败", e);
            throw new StrixOAuthException("Strix OAuth: 获取微信公众号 OAuth 授权凭证失败", e);
        }
    }

    @Override
    public Map<String, String> getUserInfo(String accessToken) {
        throw new UnsupportedOperationException("Strix OAuth: 微信公众号 OAuth 服务实例不支持获取用户信息.");
    }

    @Override
    public String getPhoneNumber(String code) {
        throw new UnsupportedOperationException("Strix OAuth: 微信公众号 OAuth 服务实例不支持获取用户手机号.");
    }

    @Override
    public boolean supportPush() {
        return true;
    }

    @Override
    public void generatePush(String openId, String content) {
        // TODO: 实现推送消息生成逻辑
    }

    @Override
    public void push(OauthPush oauthPush) {
        OkHttpClient httpClient = OkHttpUtil.getInstance();
        RequestBody requestBody = RequestBody.create(oauthPush.getContent(), MediaType.parse("application/json; charset=utf-8"));
        String pushUrl = String.format(PUSH_URL_TEMPLATE, getAccessToken());
        Request request = new Request.Builder()
                .url(pushUrl)
                .post(requestBody)
                .build();

        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                log.error("Strix OAuth: 推送消息失败.", e);
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) {
                try (response) {
                    ResponseBody body = response.body();
                    String responseStr = body.string();
                    oauthPush.setResult(responseStr);

                    Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<>() {
                    });
                    Integer errCode = MapUtil.getInt(responseMap, "errcode", -1);

                    if (errCode == 0) {
                        oauthPush.setStatus(OAuthPushStatus.SUCCESS);
                        log.debug("Strix OAuth: 推送消息成功.");
                    } else if (errCode == 40001) {
                        log.warn("Strix OAuth: 推送消息失败，AccessToken失效.");
                        return;
                    } else {
                        oauthPush.setStatus(OAuthPushStatus.FAILURE);
                        log.warn("Strix OAuth: 推送消息失败，errcode: {}, errmsg: {}", errCode, MapUtil.getStr(responseMap, "errmsg"));
                    }
                    oauthPushService.updateById(oauthPush);
                } catch (Exception e) {
                    log.error("Strix OAuth: 处理推送响应失败.", e);
                }
            }
        });
    }

    /**
     * 获取微信网页授权地址
     *
     * @param redirectUrl 授权回调地址
     * @return 微信网页授权地址
     */
    public String getAuthorizeUrl(String redirectUrl) {
        String encodedUrl = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
        return String.format(AUTHORIZE_URL_TEMPLATE, config.getAppId(), encodedUrl);
    }

}
