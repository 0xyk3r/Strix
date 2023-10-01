package cn.projectan.strix.utils.wechat.auth;

import cn.projectan.strix.model.wechat.Oauth2Token;
import cn.projectan.strix.utils.OkHttpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

/**
 * 微信相关工具类
 *
 * @author 安炯奕
 * @date 2019/11/5 16:58
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUtils {

    private final ObjectMapper objectMapper;

    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Random RANDOM = new SecureRandom();

    /**
     * 获取全局Access_Token
     *
     * @return AccessToken
     */
    public String getAccessToken(String appId, String appSecret) {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        try {
            String json = OkHttpUtil.get(url);
            log.info(json);
            Map<String, Object> resultData = objectMapper.readValue(json, new TypeReference<>() {
            });
            return resultData.get("access_token").toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取JS_API_TICKET
     *
     * @return jsApiTicket
     */
    public String getJsApiTicket(String accessToken) {
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + accessToken + "&type=jsapi";
        try {
            String json = OkHttpUtil.get(url);
            log.info(json);
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {
            });
            return data.get("ticket").toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取微信授权地址
     *
     * @return 微信授权地址
     */
    public String getAuthorizeUrl(String appId, String redirectUrl, int type) {
        String state = "STATE";
        String base = "snsapi_base";
        String userinfo = "snsapi_userinfo";
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appId + "&redirect_uri=" + redirectUrl +
                "&response_type=code&scope=" + (type == 1 ? base : userinfo) + "&state=" + state + "&connect_redirect=1#wechat_redirect";
    }


    /**
     * 获取网页授权凭证
     *
     * @param code 授权回调获取的CODE
     * @return Oauth2Token
     */
    public Oauth2Token getOauth2AccessToken(String appId, String appSecret, String code) throws JsonProcessingException {
        Oauth2Token wat = null;
        String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
        requestUrl = requestUrl.replace("APPID", appId);
        requestUrl = requestUrl.replace("SECRET", appSecret);
        requestUrl = requestUrl.replace("CODE", code);
        // 获取网页授权凭证
        Map<String, Object> data = objectMapper.readValue(OkHttpUtil.get(requestUrl), new TypeReference<>() {
        });
        if (null != data) {
            try {
                wat = new Oauth2Token();
                wat.setAccessToken(data.get("access_token").toString());
                wat.setExpiresIn(Integer.parseInt(data.get("expires_in").toString()));
                wat.setRefreshToken(data.get("refresh_token").toString());
                wat.setOpenId(data.get("openid").toString());
                wat.setScope(data.get("scope").toString());
            } catch (Exception e) {
                wat = null;
                int errorCode = Integer.parseInt(data.get("errcode").toString());
                String errorMsg = data.get("errmsg").toString();
                log.error("获取网页授权凭证失败 errcode:{} errmsg:{}", errorCode, errorMsg);
            }
        }
        return wat;
    }

    /**
     * 获取当前时间戳，单位秒
     *
     * @return 当前时间戳 单位秒
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 获取随机字符串 NonceStr
     *
     * @return 随机字符串
     */
    public static String generateNonceStr() {
        char[] nonceChars = new char[32];
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        return new String(nonceChars);
    }

}
