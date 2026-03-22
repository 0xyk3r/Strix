package cn.projectan.strix.task.system;

import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.core.module.oauth.impl.WechatOAOAuthClient;
import cn.projectan.strix.model.db.system.OauthPush;
import cn.projectan.strix.model.dict.system.OAuthPushStatus;
import cn.projectan.strix.service.system.OauthPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strix OAuth 推送任务
 *
 * @author ProjectAn
 * @since 2022/3/27 12:17
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "strix.module", name = "push", havingValue = "true")
@RequiredArgsConstructor
public class StrixOAuthPushTask {

    private final OauthPushService oauthPushService;
    private final StrixOAuthStore strixOAuthStore;

    @Scheduled(cron = "0/10 * * * * ?")
    public void sendTask() {
        try {
            List<OauthPush> pushList = oauthPushService.lambdaQuery()
                    .eq(OauthPush::getStatus, OAuthPushStatus.WAITING)
                    .list();

            for (OauthPush op : pushList) {
                StrixOAuthClient<?> client = strixOAuthStore.getInstance(op.getConfigId());
                if (client == null) {
                    log.warn("Strix OAuth Push: 未找到配置ID为 {} 的 OAuth 客户端, 跳过推送", op.getConfigId());
                    continue;
                }
                if (client instanceof WechatOAOAuthClient wechatClient && wechatClient.getAccessToken() == null) {
                    log.warn("Strix OAuth Push: 微信 OAuth 客户端 <{}> AccessToken 未就绪, 跳过本轮推送", wechatClient.getConfigName());
                    continue;
                }
                client.push(op);
            }
        } catch (Exception e) {
            log.error("OAuth 推送异常: {}", e.getMessage(), e);
        }
    }

}
