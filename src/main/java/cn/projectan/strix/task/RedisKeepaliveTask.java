package cn.projectan.strix.task;

import cn.projectan.strix.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 保活任务
 *
 * @author ProjectAn
 * @date 2022/9/9 18:45
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(RedisTemplate.class)
@RequiredArgsConstructor
public class RedisKeepaliveTask {

    private final RedisUtil redisUtil;

    @Scheduled(cron = "0/50 * * * * ?")
    public void keepalive() {
        redisUtil.ping();
    }

}
