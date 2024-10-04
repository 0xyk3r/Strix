package cn.projectan.strix.controller.wechat;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.controller.wechat.base.BaseWechatController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.core.module.oauth.WechatOAuthClient;
import cn.projectan.strix.core.module.oauth.WechatOAuthTools;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.OauthUser;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.other.module.oauth.WechatOAuthConfig;
import cn.projectan.strix.service.OauthUserService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.RedisUtil;
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
 * 微信相关api
 * <p>
 * 该类使用 @Controller 注解, 需要注意需要返回 Json 的接口，记得要加 @ResponseBody 注解.
 *
 * @author ProjectAn
 * @since 2021/8/24 16:40
 */
@Slf4j
@Controller
@RequestMapping("wechat/{configId}")
@RequiredArgsConstructor
public class WechatController extends BaseWechatController {

    @Value("${spring.profiles.active}")
    private String env;

    private final SystemUserService systemUserService;
    private final OauthUserService oauthUserService;
    private final RedisUtil redisUtil;
    private final StrixOAuthStore strixOAuthStore;

    /**
     * 统一跳转入口
     */
    @Anonymous
    @IgnoreDataEncryption
    @RequestMapping("jump/{model}")
    public void jumpToModel(@PathVariable String configId, @PathVariable String model, @RequestParam(defaultValue = "") String params, HttpServletResponse response) {
        WechatOAuthClient instance = (WechatOAuthClient) strixOAuthStore.getInstance(configId);
        WechatOAuthConfig config = (WechatOAuthConfig) instance.getConfig();
        String authorizeUrl = instance.getAuthorizeUrl(config.getAuthUrl() + configId + "/auth?model=" + model + "&params=" + params);
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
    @IgnoreDataEncryption
    @RequestMapping("auth")
    public void userAuth(@PathVariable String configId, String model, String params,
                         HttpServletRequest request, HttpServletResponse response) {
        WechatOAuthClient instance = (WechatOAuthClient) strixOAuthStore.getInstance(configId);
        WechatOAuthConfig config = (WechatOAuthConfig) instance.getConfig();
        try {
            Map<String, String[]> reqParams = request.getParameterMap();
            String[] codes = reqParams.get("code");
            if (codes == null) {
                return;
            }
            String code = codes[0];

            // 获取 OAuth 用户信息
            BaseOAuthUserInfo oAuthUserInfo = instance.grantBaseUserInfo(code);
            // 保存 OAuth 用户信息至数据库
            OauthUser oauthUser = oauthUserService.lambdaQuery()
                    .eq(OauthUser::getAppId, oAuthUserInfo.getAppId())
                    .eq(OauthUser::getOpenId, oAuthUserInfo.getOpenId())
                    .one();

            SystemUser systemUser;
            if (oauthUser == null) {
                // 如果数据库中没有 OAuth 用户信息, 则创建
                systemUser = oauthUserService.createSystemUser(oAuthUserInfo, instance.getPlatform());
            } else {
                // 如果数据库中有 OAuth 用户信息, 则获取
                systemUser = systemUserService.getSystemUser(oauthUser.getPlatform(), oauthUser.getId());
            }
            Assert.notNull(systemUser, "系统用户信息获取失败");

            // 检查之前该账号是否存在token
            Object existToken = redisUtil.get("strix:system:user:login_token:login:id_" + systemUser.getId());
            if (existToken != null) {
                // 使旧数据失效
                redisUtil.del("strix:system:user:login_token:token:" + existToken);
                redisUtil.del("strix:system:user:login_token:login:id_" + systemUser.getId());
            }
            // 生成并保存Token 有效期30天
            String token = IdUtil.simpleUUID();
            redisUtil.set("strix:system:user:login_token:login:id_" + systemUser.getId(), token, 60 * 60 * 24 * 30);
            redisUtil.set("strix:system:user:login_token:token:" + token, systemUser, 60 * 60 * 24 * 30);

            response.sendRedirect(config.getWebIndexUrl() + "?token=" + token + "&cfid=" + configId + "&tp=" + model + "&params=" + params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 初始化H5 js-sdk 使用的api
     */
    @Anonymous
    @IgnoreDataEncryption
    @ResponseBody
    @RequestMapping("config")
    public Map<String, String> config(@PathVariable String configId, String webUrl) {
        WechatOAuthClient instance = (WechatOAuthClient) strixOAuthStore.getInstance(configId);
        WechatOAuthConfig config = (WechatOAuthConfig) instance.getConfig();
        try {
            if (!"dev".equals(env)) {
                Assert.isTrue(StringUtils.hasText(webUrl) && (webUrl.startsWith(config.getWebIndexUrl())), "域名不合法");
            }
            Map<String, String> signMap = new HashMap<>();
            signMap.put("jsapi_ticket", instance.getJsApiTicket());
            signMap.put("noncestr", WechatOAuthTools.generateNonceStr());
            signMap.put("timestamp", String.valueOf(WechatOAuthTools.getCurrentTimestamp()));
            signMap.put("url", webUrl);

            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("appId", config.getAppId());
            resultMap.put("timestamp", signMap.get("timestamp"));
            resultMap.put("nonceStr", signMap.get("noncestr"));
            resultMap.put("signature", WechatOAuthTools.signBySha1(signMap));

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
    @IgnoreDataEncryption
    @RequestMapping("giveMeSessionTokenOnDevMode")
    public void devMode(@PathVariable String configId, HttpServletResponse response) throws IOException {
        if ("dev".equals(env)) {
            log.warn("通过api获取微信Token...");

            SystemUser systemUser = systemUserService.getById("1775599867535130625");

            // 检查之前该账号是否存在token
            Object existToken = redisUtil.get("strix:system:user:login_token:login:id_" + systemUser.getId());
            if (existToken != null) {
                // 使旧数据失效
                redisUtil.del("strix:system:user:login_token:token:" + existToken);
                redisUtil.del("strix:system:user:login_token:login:id_" + systemUser.getId());
            }
            // 生成并保存Token 有效期30天
            String token = IdUtil.simpleUUID();
            redisUtil.set("strix:system:user:login_token:login:id_" + systemUser.getId(), token, 60 * 60 * 24 * 30);
            redisUtil.set("strix:system:user:login_token:token:" + token, systemUser, 60 * 60 * 24 * 30);

            response.sendRedirect("http://localhost:8080/?token=" + token + "&cfid=" + configId);
        }
    }

    /**
     * 检查Token是否有效
     */
    @ResponseBody
    @RequestMapping("checkToken")
    public RetResult<Object> checkToken() {
        return RetBuilder.success();
    }

}
