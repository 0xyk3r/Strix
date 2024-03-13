package cn.projectan.strix.config;

import cn.projectan.strix.model.db.WechatConfig;
import cn.projectan.strix.model.wechat.WechatConfigBean;
import cn.projectan.strix.utils.wechat.auth.WechatUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ProjectAn
 * @date 2021/8/24 18:53
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalWechatConfig {

    private final WechatUtils wechatUtils;

    private final Map<String, WechatConfigBean> wechatConfigInstanceMap = new HashMap<>();

    public void addInstance(WechatConfig wechatConfig) {
        WechatConfigBean wechatConfigBean = new WechatConfigBean();
        wechatConfigBean.setId(wechatConfig.getId());
        wechatConfigBean.setName(wechatConfig.getName());
        wechatConfigBean.setAppId(wechatConfig.getAppId());
        wechatConfigBean.setAppSecret(wechatConfig.getAppSecret());
        wechatConfigBean.setToken(wechatConfig.getToken());
        wechatConfigBean.setWebIndexUrl(wechatConfig.getWebIndexUrl());
        wechatConfigBean.setAuthUrl(wechatConfig.getAuthUrl());
        wechatConfigBean.setAccessToken(wechatUtils.getAccessToken(wechatConfigBean.getAppId(), wechatConfigBean.getAppSecret()));
        wechatConfigBean.setJsApiTicket(wechatUtils.getJsApiTicket(wechatConfigBean.getAccessToken()));
        wechatConfigInstanceMap.put(wechatConfig.getId(), wechatConfigBean);
        Thread thread = new Thread(new RefreshAccessTokenThread(wechatConfigBean));
        thread.setName("RefreshAccessTokenThread-" + wechatConfigBean.getId());
        thread.start();
    }

    public WechatConfigBean getInstance(String id) {
        return wechatConfigInstanceMap.get(id);
    }

    private class RefreshAccessTokenThread implements Runnable {

        private final WechatConfigBean wechatConfigBean;

        public RefreshAccessTokenThread(WechatConfigBean wechatConfigBean) {
            this.wechatConfigBean = wechatConfigBean;
        }

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(1000 * 60 * 60);
                wechatConfigBean.setAccessToken(wechatUtils.getAccessToken(wechatConfigBean.getAppId(), wechatConfigBean.getAppSecret()));
                wechatConfigBean.setJsApiTicket(wechatUtils.getJsApiTicket(wechatConfigBean.getAccessToken()));
                log.info("刷新" + wechatConfigBean.getName() + "公众号的AccessToken和JsApiTicket成功...");
            }
        }
    }

}
