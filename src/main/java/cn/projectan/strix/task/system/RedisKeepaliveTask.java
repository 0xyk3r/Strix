package cn.projectan.strix.task.system;

import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 保活任务
 *
 * @author ProjectAn
 * @since 2022/9/9 18:45
 */
@Slf4j
@Component
@ConditionalOnBean(RedisTemplate.class)
@RequiredArgsConstructor
public class RedisKeepaliveTask {

    private final RedisUtil redisUtil;

    @Scheduled(cron = "0/50 * * * * ?")
    public void keepalive() {
        redisUtil.ping();
    }

}
