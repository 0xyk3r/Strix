package cn.projectan.strix.config;

import cn.projectan.strix.core.cache.CacheInvalidationBroadcaster;
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置类
 *
 * @author ProjectAn
 * @since 2021/05/02 16:41
 */
@Configuration
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * RedisTemplate 配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer = typedJacksonJsonRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jacksonJsonRedisSerializer);
        template.setHashValueSerializer(jacksonJsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置使用 @Cacheable 注解的序列化方式
     * <p>默认是 JDK 序列化, 加上此配置则为 JSON 序列化
     * <p>默认缓存时长为1天
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(getRedisCacheConfigurationWithTtl(60 * 60 * 24))
                .withInitialCacheConfigurations(getRedisCacheConfigurationMap())
                .build();
    }

    /**
     * 缓存时长配置
     * <p>由于 @Cacheable 注解不支持配置缓存时长，所以需要在此进行配置
     */
    private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        // 字典: 7 天 (低频变更)
        redisCacheConfigurationMap.put("dict_data", getRedisCacheConfigurationWithTtl(60 * 60 * 24 * 7));
        redisCacheConfigurationMap.put("dict_version", getRedisCacheConfigurationWithTtl(60 * 60 * 24 * 7));
        // 认证/菜单/权限/角色: 1 天 (默认)
        redisCacheConfigurationMap.put("select_data", getRedisCacheConfigurationWithTtl(60 * 60 * 24));
        redisCacheConfigurationMap.put("menu_by_rid", getRedisCacheConfigurationWithTtl(60 * 60 * 24));
        redisCacheConfigurationMap.put("permission_by_rid", getRedisCacheConfigurationWithTtl(60 * 60 * 24));
        redisCacheConfigurationMap.put("menu_by_mid", getRedisCacheConfigurationWithTtl(60 * 60 * 24));
        redisCacheConfigurationMap.put("permission_by_mid", getRedisCacheConfigurationWithTtl(60 * 60 * 24));
        // 系统配置: 1 小时 (中频变更)
        redisCacheConfigurationMap.put("strix:config", getRedisCacheConfigurationWithTtl(60 * 60));
        // 地区: 7 天 (低频变更)
        redisCacheConfigurationMap.put("strix:region_by_id", getRedisCacheConfigurationWithTtl(60 * 60 * 24 * 7));
        redisCacheConfigurationMap.put("strix:region_children", getRedisCacheConfigurationWithTtl(60 * 60 * 24 * 7));

        return redisCacheConfigurationMap;
    }

    /**
     * 设置序列化方式以及缓存时长
     *
     * @param seconds 缓存时长
     * @return RedisCacheConfiguration
     */
    private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(long seconds) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(typedJacksonJsonRedisSerializer()))
                .entryTtl(Duration.ofSeconds(seconds));
        return config;
    }

    /**
     * 设置 Redis 序列化方式为 Jackson（带类型信息）
     * <p>用于 @Cacheable 注解，在 JSON 中包含 @class 字段以便正确反序列化
     */
    private JacksonJsonRedisSerializer<Object> typedJacksonJsonRedisSerializer() {
        // 自定义类型验证器，明确允许的类型范围
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                // 允许项目包下的所有类型
                .allowIfSubType("cn.projectan.strix.")
                // 允许常用 Java 集合类型
                .allowIfSubType("java.util.")
                // 允许数组类型
                .allowIfSubTypeIsArray()
                .build();

        ObjectMapper objectMapper = JacksonConfig.builder()
                .changeDefaultVisibility(vp -> vp.with(JsonAutoDetect.Visibility.ANY))
                .activateDefaultTypingAsProperty(typeValidator, DefaultTyping.NON_FINAL_AND_ENUMS, "@class")
                .build();

        return new JacksonJsonRedisSerializer<>(objectMapper, Object.class);
    }

    /**
     * Redis Pub/Sub 消息监听容器
     * 用于缓存失效广播的订阅
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            CacheInvalidationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(CacheInvalidationBroadcaster.CHANNEL));
        return container;
    }

}
