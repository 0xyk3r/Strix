package cn.projectan.strix.controller.srv.wechat;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.controller.srv.wechat.base.BaseWechatController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.core.module.oauth.impl.WechatOAOAuthClient;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.model.db.system.OauthUser;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.system.module.oauth.wechat.oa.WechatOAOAuthConfig;
import cn.projectan.strix.service.system.OauthUserService;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.module.oauth.WechatOAOAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号相关 API 控制器
 * <p>
 * 该类使用 @Controller 注解, 需要注意需要返回 Json 的接口，记得要加 @ResponseBody 注解.
 *
 * @author ProjectAn
 * @since 2021/8/24 16:40
 */
@Slf4j
@Controller
@RequestMapping("api/wechat/oa/{configKey}")
@RequiredArgsConstructor
public class WechatOAController extends BaseWechatController {

    @Value("${spring.profiles.active}")
    private String env;

    private final SystemUserService systemUserService;
    private final OauthUserService oauthUserService;
    private final StrixOAuthStore strixOAuthStore;
    private final RedisUtil redisUtil;

    /**
     * 统一跳转入口
     */
    @Anonymous
    @IgnoreEncryption
    @RequestMapping("jump/{model}")
    public void jumpToModel(@PathVariable String configKey, @PathVariable String model, @RequestParam(defaultValue = "") String params, HttpServletResponse response) {
        WechatOAOAuthClient instance = (WechatOAOAuthClient) strixOAuthStore.getInstance(configKey, OAuthPlatform.WECHAT_OA);
        WechatOAOAuthConfig config = (WechatOAOAuthConfig) instance.getConfig();
        String authorizeUrl = instance.getAuthorizeUrl(config.getAuthUrl() + configKey + "/auth?model=" + model + "&params=" + params);
        try {
            response.sendRedirect(authorizeUrl);
        } catch (Exception e) {
            throw new StrixException("跳转失败");
        }
    }

    /**
     * 统一授权接口
     */
    @Anonymous
    @IgnoreEncryption
    @RequestMapping("auth")
    public void userAuth(@PathVariable String configKey, String model, String params,
                         HttpServletRequest request, HttpServletResponse response) {
        WechatOAOAuthClient instance = (WechatOAOAuthClient) strixOAuthStore.getInstance(configKey);
        WechatOAOAuthConfig config = (WechatOAOAuthConfig) instance.getConfig();
        try {
            Map<String, String[]> reqParams = request.getParameterMap();
            String[] codes = reqParams.get("code");
            if (codes == null) {
                return;
            }
            String code = codes[0];

            // 获取 OAuth 用户信息
            BaseOAuthUserInfo oAuthUserInfo = instance.auth(code);
            // 保存 OAuth 用户信息至数据库
            OauthUser oauthUser = oauthUserService.lambdaQuery()
                    .eq(OauthUser::getAppId, oAuthUserInfo.getAppId())
                    .eq(OauthUser::getOpenId, oAuthUserInfo.getOpenId())
                    .one();

            SystemUser systemUser;
            if (oauthUser == null) {
                // 如果数据库中没有 OAuth 用户信息, 则创建
                systemUser = oauthUserService.loginOrCreateSystemUser(oAuthUserInfo, instance.getPlatform());
            } else {
                // 如果数据库中有 OAuth 用户信息, 则获取
                systemUser = systemUserService.getSystemUser(oauthUser.getPlatform(), oauthUser.getId());
            }
            Assert.notNull(systemUser, "系统用户信息获取失败");

            // 检查之前该账号是否存在token
            Object existToken = redisUtil.get(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId());
            if (existToken != null) {
                // 使旧数据失效
                redisUtil.del(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + existToken);
                redisUtil.del(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId());
            }
            // 生成并保存Token 有效期30天
            String token = IdUtil.simpleUUID();
            redisUtil.set(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId(), token, 60 * 60 * 24 * 30);
            redisUtil.set(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + token, systemUser, 60 * 60 * 24 * 30);

            response.sendRedirect(config.getWebIndexUrl() + "?token=" + token + "&cfid=" + configKey + "&tp=" + model + "&params=" + params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 初始化H5 js-sdk 使用的api
     */
    @Anonymous
    @IgnoreEncryption
    @ResponseBody
    @RequestMapping("config")
    public Map<String, String> config(@PathVariable String configKey, String webUrl) {
        WechatOAOAuthClient instance = (WechatOAOAuthClient) strixOAuthStore.getInstance(configKey);
        WechatOAOAuthConfig config = (WechatOAOAuthConfig) instance.getConfig();
        try {
            if (!"dev".equals(env)) {
                Assert.isTrue(StringUtils.hasText(webUrl) && (webUrl.startsWith(config.getWebIndexUrl())), "域名不合法");
            }
            Map<String, String> signMap = new HashMap<>();
            signMap.put("jsapi_ticket", instance.getJsApiTicket());
            signMap.put("noncestr", WechatOAOAuthUtil.generateNonceStr());
            signMap.put("timestamp", String.valueOf(WechatOAOAuthUtil.getCurrentTimestamp()));
            signMap.put("url", webUrl);

            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("appId", config.getAppId());
            resultMap.put("timestamp", signMap.get("timestamp"));
            resultMap.put("nonceStr", signMap.get("noncestr"));
            resultMap.put("signature", WechatOAOAuthUtil.signBySha1(signMap));

            return resultMap;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 本地开发时使用
     */
    @Anonymous
    @IgnoreEncryption
    @RequestMapping("giveMeSessionTokenOnDevMode")
    public void devMode(@PathVariable String configKey, HttpServletResponse response) throws IOException {
        if ("dev".equals(env)) {
            log.warn("通过api获取微信Token...");

            SystemUser systemUser = systemUserService.getById("1775599867535130625");

            // 检查之前该账号是否存在token
            Object existToken = redisUtil.get(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId());
            if (existToken != null) {
                // 使旧数据失效
                redisUtil.del(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + existToken);
                redisUtil.del(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId());
            }
            // 生成并保存Token 有效期30天
            String token = IdUtil.simpleUUID();
            redisUtil.set(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + systemUser.getId(), token, 60 * 60 * 24 * 30);
            redisUtil.set(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + token, systemUser, 60 * 60 * 24 * 30);

            response.sendRedirect("http://localhost:8080/?token=" + token + "&cfid=" + configKey);
        }
    }

    /**
     * 检查Token是否有效
     */
    @ResponseBody
    @RequestMapping("checkToken")
    public RetResult<Object> checkToken(@PathVariable String configKey) {
        return RetBuilder.success();
    }

}
