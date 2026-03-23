package cn.projectan.strix.util.module.oauth;

import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.WechatMPOAuthConfig;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.req.WechatMPGetPhoneNumberReq;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp.WechatMPAuthResp;
import cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp.WechatMPGetPhoneNumberResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import cn.projectan.strix.util.http.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;

/**
 * @author ProjectAn
 * @since 2026/1/29 09:48
 */
@Slf4j
public class WechatMPOAuthUtil {

    private static final String WECHAT_MP_AUTH_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String WECHAT_MP_GET_PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";

    public static BaseOAuthUserInfo auth(WechatMPOAuthConfig config, String code) {
        // docs: https://developers.weixin.qq.com/miniprogram/dev/server/API/user-login/api_code2session.html
        Map<String, String> params = Map.of(
                "appid", config.getAppId(),
                "secret", config.getAppSecret(),
                "js_code", code,
                "grant_type", "authorization_code"
        );

        String response = OkHttpUtil.get(WECHAT_MP_AUTH_URL, params);
        WechatMPAuthResp resp = ObjectMapperUtil.readValue(response, WechatMPAuthResp.class);
        Assert.notNull(resp, I18nUtil.get("assert.oauth.wechatMPAuthFailed"));

        return new BaseOAuthUserInfo(
                config.getId(),
                config.getAppId(),
                resp.getSessionKey(),
                null,
                resp.getOpenId(),
                resp.getUnionId(),
                null
        );
    }

    public static String getPhoneNumber(String accessToken, String code) {
        // docs: https://developers.weixin.qq.com/miniprogram/dev/server/API/user-info/phone-number/api_getphonenumber.html
        WechatMPGetPhoneNumberReq req = new WechatMPGetPhoneNumberReq(code);

        String response = OkHttpUtil.postJson(
                String.format(WECHAT_MP_GET_PHONE_NUMBER_URL, accessToken),
                ObjectMapperUtil.writeValue(req)
        );
        WechatMPGetPhoneNumberResp resp = ObjectMapperUtil.readValue(response, WechatMPGetPhoneNumberResp.class);

        String phoneNumber = Optional.ofNullable(resp)
                .map(WechatMPGetPhoneNumberResp::getPhoneInfo)
                .map(WechatMPGetPhoneNumberResp.PhoneInfo::getPhoneNumber)
                .orElse(null);
        Assert.hasText(phoneNumber, I18nUtil.get("assert.oauth.wechatMPPhoneFailed"));
        return phoneNumber;
    }

}
