package cn.projectan.strix.core.module.oauth.impl;

import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthConfig;
import cn.projectan.strix.util.common.SpringUtil;
import cn.projectan.strix.util.module.oauth.WechatCommonUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 微信 OAuth 客户端基类
 * 提供统一的 AccessToken 管理逻辑
 *
 * @author ProjectAn
 * @since 2026/1/29
 */
@Slf4j
public abstract class AbstractWechatOAuthClient<T extends BaseOAuthConfig> extends StrixOAuthClient<T> {

    @Getter
    @Setter
    protected String accessToken;

    protected AbstractWechatOAuthClient(T config) {
        super(config);
    }

    /**
     * 初始化 AccessToken 刷新任务
     *
     * @param appId     微信 AppId
     * @param appSecret 微信 AppSecret
     */
    protected void initAccessTokenRefreshTask(String appId, String appSecret) {
        Runnable refreshTask = createRefreshTask(appId, appSecret);
        // 立即执行一次
        refreshTask.run();

        // 定时刷新（每60分钟）
        ScheduledExecutorService scheduler = SpringUtil.getBean("strixOAuthScheduler", ScheduledExecutorService.class);
        scheduler.scheduleWithFixedDelay(refreshTask, 60, 60, TimeUnit.MINUTES);
    }

    /**
     * 创建 AccessToken 刷新任务
     *
     * @param appId     微信 AppId
     * @param appSecret 微信 AppSecret
     * @return 刷新任务
     */
    private Runnable createRefreshTask(String appId, String appSecret) {
        return () -> {
            Thread.currentThread().setName("strix-oauth-" + config.getId());
            try {
                String newAccessToken = WechatCommonUtil.getAccessToken(appId, appSecret);
                setAccessToken(newAccessToken);
                onAccessTokenRefreshed(newAccessToken);
                log.info("Strix OAuth: 刷新微信实例 <{}> 的 AccessToken 完成.", config.getName());
            } catch (Exception e) {
                log.error("Strix OAuth: 刷新微信实例 <{}> 的 AccessToken 发生异常.", config.getName(), e);
            }
        };
    }

    /**
     * AccessToken 刷新后的回调方法
     * 子类可以覆盖此方法以执行额外的操作（如刷新 JsApiTicket）
     *
     * @param newAccessToken 新的 AccessToken
     */
    protected void onAccessTokenRefreshed(String newAccessToken) {
        // 默认不执行任何操作
    }

}
