package cn.projectan.strix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * @author ProjectAn
 * @since 2024-10-15 16:51:34
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setThreads(0) //线程池数量
                    .setNettyThreads(0) // Netty线程池数量
                    .setCodec(new JsonJacksonCodec(objectMapper));
            config.useSingleServer() // 使用单机模式
//                    .setNameMapper(new KeyPrefixHandler("strix")) // 设置 redis key 前缀
                    .setTimeout(10000)
                    .setClientName("strix")
                    .setIdleConnectionTimeout(10000) // 连接空闲超时，单位：毫秒
                    .setSubscriptionConnectionPoolSize(50) // 发布和订阅连接池大小
                    .setConnectionMinimumIdleSize(8) // 最小空闲连接数
                    .setConnectionPoolSize(32); // 连接池大小
        };
    }

}
