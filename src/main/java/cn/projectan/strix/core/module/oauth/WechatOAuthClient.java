package cn.projectan.strix.core.module.oauth;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.model.db.OauthPush;
import cn.projectan.strix.model.dict.OAuthPushStatus;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthConfig;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.module.oauth.WechatOAuthConfig;
import cn.projectan.strix.service.OauthPushService;
import cn.projectan.strix.utils.OkHttpUtil;
import cn.projectan.strix.utils.SpringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信 OAuth 客户端
 *
 * @author ProjectAn
 * @since 2024/4/3 17:34
 */
@Slf4j
public class WechatOAuthClient extends StrixOAuthClient {

    protected final WechatOAuthConfig config;
    private final ObjectMapper objectMapper;
    private final OauthPushService oauthPushService;

    @Getter
    protected String accessToken;
    @Getter
    protected String jsApiTicket;

    public WechatOAuthClient(WechatOAuthConfig config) {
        super();
        Assert.notNull(config, "Strix OAuth: 初始化微信 OAuth 服务实例失败. (配置信息为空)");
        this.config = config;
        this.objectMapper = SpringUtil.getBean(ObjectMapper.class);
        this.oauthPushService = SpringUtil.getBean(OauthPushService.class);
        try {
            refreshAccessToken();
            refreshJsApiTicket();
            Thread thread = new Thread(new WechatOAuthClient.RefreshAccessTokenThread());
            thread.setName("Strix OAuth RAT - " + config.getId());
            thread.start();
        } catch (Exception e) {
            throw new RuntimeException("Strix OAuth: 初始化微信 OAuth 服务实例失败. (配置信息错误)", e);
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
        return true;
    }

    @Override
    public BaseOAuthConfig getConfig() {
        return config;
    }

    @Override
    public BaseOAuthUserInfo grantBaseUserInfo(String code) {
        String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
        requestUrl = requestUrl.replace("APPID", config.getAppId());
        requestUrl = requestUrl.replace("SECRET", config.getAppSecret());
        requestUrl = requestUrl.replace("CODE", code);
        // 获取网页授权凭证
        try {
            Map<String, Object> data = objectMapper.readValue(OkHttpUtil.get(requestUrl), new TypeReference<>() {
            });
            Assert.notNull(data, "Strix OAuth: 获取微信 OAuth 授权凭证失败.");

            try {
                BaseOAuthUserInfo oAuthUserInfo = new BaseOAuthUserInfo();
                oAuthUserInfo.setConfigId(config.getId());
                oAuthUserInfo.setAppId(config.getAppId());
                oAuthUserInfo.setOpenId(MapUtil.getStr(data, "openid"));
                Assert.hasText(oAuthUserInfo.getOpenId(), "Strix OAuth: 获取微信 OAuth 授权凭证失败, OpenId 为空.");
                oAuthUserInfo.setAccessToken(MapUtil.getStr(data, "access_token"));
                oAuthUserInfo.setRefreshToken(MapUtil.getStr(data, "refresh_token"));
                oAuthUserInfo.setExpiresIn(MapUtil.getInt(data, "expires_in"));
                oAuthUserInfo.setUnionId(MapUtil.getStr(data, "unionid"));
                return oAuthUserInfo;
            } catch (Exception e) {
                Integer errorCode = MapUtil.getInt(data, "errcode");
                String errorMsg = MapUtil.getStr(data, "errmsg");
                log.error("Strix OAuth: 获取微信 OAuth 授权凭证失败, errcode:{} errmsg:{}", errorCode, errorMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 OAuth 授权凭证失败", e);
            return null;
        }
    }

    @Override
    public Map<String, String> grantMoreUserInfo(String accessToken) {
        throw new UnsupportedOperationException("Strix OAuth: 微信 OAuth 服务实例不支持获取用户信息.");
    }

    @Override
    public void generatePush(String openId, String content) {

    }

    @Override
    public void push(OauthPush oauthPush) {
        OkHttpClient httpClient = OkHttpUtil.getInstance();
        RequestBody requestBody = RequestBody.create(oauthPush.getContent(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + getAccessToken()).post(requestBody).build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error(e.getMessage(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseStr = response.body().string();
                oauthPush.setResult(responseStr);
                Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<>() {
                });
                Integer errCode = MapUtil.getInt(responseMap, "errcode", -1);
                if (errCode == 0) {
                    // 推送成功
                    oauthPush.setStatus(OAuthPushStatus.SUCCESS);
                } else if (errCode == 40001) {
                    // AccessToken失效 不做任何处理 自行重试
                    log.info("推送消息失败，AccessToken失效.");
                    return;
                } else if (responseStr.contains("block")) {
                    log.info("推送消息失败，block.");
                    oauthPush.setStatus(OAuthPushStatus.FAILURE);
                } else if (responseStr.contains("failed")) {
                    log.info("推送消息失败，failed.");
                    oauthPush.setStatus(OAuthPushStatus.FAILURE);
                } else {
                    log.info("推送消息失败，其他错误. {}", responseStr);
                    oauthPush.setStatus(OAuthPushStatus.FAILURE);
                }
                oauthPushService.updateById(oauthPush);
            }
        });

    }

    /**
     * 获取全局Access_Token
     */
    public void refreshAccessToken() {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + config.getAppId() + "&secret=" + config.getAppSecret();
        try {
            String responseStr = OkHttpUtil.get(url);
            Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<>() {
            });
            Assert.notNull(responseMap, "Strix OAuth: 获取微信 AccessToken 失败.");
            String accessToken = MapUtil.getStr(responseMap, "access_token");
            Assert.hasText(accessToken, "Strix OAuth: 获取微信 AccessToken 失败.");
            this.accessToken = accessToken;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 AccessToken 失败", e);
        }
    }

    /**
     * 获取JS_API_TICKET
     */
    public void refreshJsApiTicket() {
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + this.accessToken + "&type=jsapi";
        try {
            String responseStr = OkHttpUtil.get(url);
            Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<>() {
            });
            Assert.notNull(responseMap, "Strix OAuth: 获取微信 JsApiTicket 失败.");
            String ticket = MapUtil.getStr(responseMap, "ticket");
            Assert.hasText(accessToken, "Strix OAuth: 获取微信 JsApiTicket 失败.");
            this.jsApiTicket = ticket;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 JsApiTicket 失败", e);
        }
    }

    /**
     * 获取微信网页授权地址
     *
     * @return 微信网页授权地址
     */
    public String getAuthorizeUrl() {
        return getAuthorizeUrl(config.getAuthUrl());
    }

    public String getAuthorizeUrl(String redirectUrl) {
        String url = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + config.getAppId() + "&redirect_uri=" + url +
                "&response_type=code&scope=snsapi_base&state=ProjectAn#wechat_redirect";
    }

    private class RefreshAccessTokenThread implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(1000 * 60 * 60);
                refreshAccessToken();
                refreshJsApiTicket();
                log.info("Strix OAuth: 刷新实例 <{}> 的 AccessToken 和 JsApiTicket 完成.", config.getName());
            }
        }
    }

}
