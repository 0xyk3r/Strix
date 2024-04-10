package cn.projectan.strix.task;

import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.model.db.OauthPush;
import cn.projectan.strix.model.dict.OAuthPushStatus;
import cn.projectan.strix.service.OauthPushService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2022/3/27 12:17
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "strix.module", name = "push", havingValue = "true")
@RequiredArgsConstructor
public class StrixOAuthPushTask {

    private final OauthPushService oauthPushService;
    private final StrixOAuthStore strixOAuthStore;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0/10 * * * * ?")
    public void sendTask() {
        try {
            QueryWrapper<OauthPush> qw = new QueryWrapper<>();
            qw.eq("`status`", OAuthPushStatus.WAITING);
            List<OauthPush> pushList = oauthPushService.list(qw);

            for (OauthPush op : pushList) {
                StrixOAuthClient client = strixOAuthStore.getInstance(op.getConfigId());
                client.push(op);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
