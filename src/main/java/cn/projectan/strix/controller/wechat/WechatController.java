package cn.projectan.strix.controller.wechat;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.config.GlobalWechatConfig;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.WechatUser;
import cn.projectan.strix.model.wechat.Oauth2Token;
import cn.projectan.strix.model.wechat.WechatConfigBean;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.service.WechatUserService;
import cn.projectan.strix.utils.RedisUtil;
import cn.projectan.strix.utils.WechatSignUtil;
import cn.projectan.strix.utils.WechatUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信相关api
 * <p>
 * 备注：操他妈的由于这个类部分接口需要重定向Controller上面用的是@Controller。需要注意需要返回Json的接口，记得要加@ResponseBody
 *
 * @author 安炯奕
 * @date 2021/8/24 16:40
 */
@Slf4j
@Controller
@RequestMapping("wechat/{wechatConfigId}")
public class WechatController {

    @Value("${spring.profiles.active}")
    private String profiles;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private WechatUtils wechatUtils;
    @Autowired
    private GlobalWechatConfig globalWechatConfig;

    /**
     * 统一跳转入口
     */
    @IgnoreDataEncryption
    @RequestMapping("jump/{model}")
    public void jumpToModel(@PathVariable String wechatConfigId, @PathVariable String model, String params, HttpServletResponse response) {
        WechatConfigBean wechatConfigBean = globalWechatConfig.getInstance(wechatConfigId);
        try {
            if (params == null) {
                params = "";
            }
            response.sendRedirect(wechatUtils.getAuthorizeUrl(wechatConfigBean.getAppId(), URLEncoder.encode(wechatConfigBean.getAuthUrl() + wechatConfigId + "/auth?model=" + model + "&params=" + params, "utf-8"), 1));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 统一授权接口
     * TODO 后续改为独立的授权服务，以应对微信只允许设置两个授权回调域名
     */
    @IgnoreDataEncryption
    @RequestMapping("auth")
    public void userAuth(@PathVariable String wechatConfigId, String model, String params,
                         HttpServletRequest request, HttpServletResponse response) {
        WechatConfigBean wechatConfigBean = globalWechatConfig.getInstance(wechatConfigId);
        try {
            Map<String, String[]> reqParams = request.getParameterMap();
            String[] codes = reqParams.get("code");
            if (codes == null) {
                return;
            }
            String code = codes[0];

            // 获取网页授权access_token
            Oauth2Token oauth2Token = wechatUtils.getOauth2AccessToken(wechatConfigBean.getAppId(), wechatConfigBean.getAppSecret(), code);

            // 保存至数据库
            QueryWrapper<WechatUser> wechatUserQueryWrapper = new QueryWrapper<>();
            wechatUserQueryWrapper.eq("app_id", wechatConfigBean.getAppId());
            wechatUserQueryWrapper.eq("open_id", oauth2Token.getOpenId());
            WechatUser wechatUser = wechatUserService.getOne(wechatUserQueryWrapper);
            if (wechatUser == null) {
                // 新建微信用户信息 并创建系统用户 并绑定
                wechatUser = wechatUserService.createWechatUser(oauth2Token.getOpenId(), wechatConfigBean);
            }

            // 获取SystemUser
            SystemUser systemUser = systemUserService.getSystemUser(1, wechatUser.getId());
            Assert.notNull(systemUser, "系统用户创建异常");

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

            redirect(wechatConfigBean.getWebIndexUrl(), model, params, token, wechatConfigId, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 通用跳转方法
     */
    private void redirect(String webIndexUrl, String model, String params, String token, String wechatConfigId, HttpServletResponse response) {
        try {
            response.sendRedirect(webIndexUrl + "?token=" + token + "&cfid=" + wechatConfigId + "&tp=" + model + "&params=" + params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 初始化H5 js-sdk 使用的api
     */
    @IgnoreDataEncryption
    @ResponseBody
    @RequestMapping("config")
    public Map<String, String> config(@PathVariable String wechatConfigId, String webUrl) {
        WechatConfigBean wechatConfigBean = globalWechatConfig.getInstance(wechatConfigId);
        String webIndexUrl = wechatConfigBean.getWebIndexUrl();

        try {
            if (!"dev".equals(profiles)) {
                Assert.isTrue(StringUtils.hasText(webUrl) && (webUrl.startsWith(webIndexUrl)), "域名不合法");
            }

            Map<String, String> signMap = new HashMap<>();
            signMap.put("jsapi_ticket", wechatConfigBean.getJsApiTicket());
            signMap.put("noncestr", WechatUtils.generateNonceStr());
            signMap.put("timestamp", String.valueOf(WechatUtils.getCurrentTimestamp()));
            signMap.put("url", webUrl);

            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("appId", wechatConfigBean.getAppId());
            resultMap.put("timestamp", signMap.get("timestamp"));
            resultMap.put("nonceStr", signMap.get("noncestr"));
            resultMap.put("signature", WechatSignUtil.signBySha1(signMap));

            return resultMap;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 本地开发时使用
     */
    @IgnoreDataEncryption
    @RequestMapping("giveMeSessionTokenOnDevMode")
    public void devMode(@PathVariable String wechatConfigId, HttpServletResponse response) throws IOException {
        if ("dev".equals(profiles)) {
            log.warn("通过api获取微信Token...");

            SystemUser systemUser = systemUserService.getById("1629392552362844162");

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

            response.sendRedirect("http://localhost:8080/?token=" + token + "&cfid=" + wechatConfigId);
        }
    }

    @ResponseBody
    @RequestMapping("checkToken")
    public RetResult<Object> checkToken() {
        return RetMarker.makeSuccessRsp();
    }

}
