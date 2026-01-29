package cn.projectan.strix.util.module.oauth;

import cn.projectan.strix.core.exception.StrixOAuthException;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.req.WechatGetAccessTokenReq;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp.WechatGetAccessTokenResp;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import cn.projectan.strix.util.http.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * 微信 OAuth 工具类
 *
 * @author ProjectAn
 * @since 2026/1/29 10:35
 */
@Slf4j
public class WechatCommonUtil {

    private static final String GET_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";

    /**
     * 获取全局 AccessToken
     *
     * @param appId     AppID
     * @param appSecret AppSecret
     * @return AccessToken
     * @throws StrixOAuthException 获取失败时抛出异常
     */
    public static String getAccessToken(String appId, String appSecret) {
        try {
            WechatGetAccessTokenReq req = new WechatGetAccessTokenReq("client_credential", appId, appSecret, false);

            String response = OkHttpUtil.postJson(GET_ACCESS_TOKEN_URL, ObjectMapperUtil.writeValue(req));
            Assert.hasText(response, "Strix OAuth: 获取微信 AccessToken 时远程服务器返回数据为空.");

            WechatGetAccessTokenResp resp = ObjectMapperUtil.readValue(response, WechatGetAccessTokenResp.class);
            Assert.notNull(resp, "Strix OAuth: 获取微信 AccessToken 时远程服务器返回数据异常.");
            Assert.hasText(resp.getAccessToken(), "Strix OAuth: 获取微信 AccessToken 时远程服务器返回数据异常.");

            log.debug("Strix OAuth: 获取微信 AccessToken 成功.");
            return resp.getAccessToken();
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 AccessToken 失败", e);
            throw new StrixOAuthException("Strix OAuth: 获取微信 AccessToken 失败", e);
        }
    }

}
