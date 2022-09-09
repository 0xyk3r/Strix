package cn.projectan.strix.task;

import cn.projectan.strix.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2022/9/9 18:45
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(RedisTemplate.class)
public class RedisKeepAlive {

    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0/50 * * * * ?")
    public void keepalive() {
        redisUtil.get("keepalive");
    }

}
