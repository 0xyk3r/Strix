package cn.projectan.strix.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Redis 配置类
 *
 * @author ProjectAn
 * @since 2021/05/02 16:41
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {

    private final JavaTimeModule javaTimeModule;

    /**
     * RedisTemplate 配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = jackson2JsonRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置使用 @Cacheable 注解的序列化方式
     * <p>默认是 JDK 序列化, 加上此配置则为 JSON 序列化
     * <p>默认缓存时长为30天
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(getRedisCacheConfigurationWithTtl(60 * 60 * 24 * 30))
                .withInitialCacheConfigurations(getRedisCacheConfigurationMap())
                .build();
    }

    /**
     * 缓存时长配置
     * <p>由于 @Cacheable 注解不支持配置缓存时长，所以需要在此进行配置
     */
    private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
//        redisCacheConfigurationMap.put("strix:system:manager:permission_by_mid:*", this.getRedisCacheConfigurationWithTtl(60 * 60 * 24));
//        redisCacheConfigurationMap.put("strix:system:manager:menu_by_mid:*", this.getRedisCacheConfigurationWithTtl(60 * 60 * 24));
//        redisCacheConfigurationMap.put("strix:system:role:menu_by_rid:*", this.getRedisCacheConfigurationWithTtl(60 * 60 * 24));
//        redisCacheConfigurationMap.put("strix:system:role:permission_by_rid:*", this.getRedisCacheConfigurationWithTtl(60 * 60 * 24));

        return redisCacheConfigurationMap;
    }

    /**
     * 设置序列化方式以及缓存时长
     *
     * @param seconds 缓存时长
     * @return RedisCacheConfiguration
     */
    private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(Integer seconds) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofSeconds(seconds));
        return config;
    }

    /**
     * 设置 Redis 序列化方式为 Jackson
     * <p>使用 JacksonConfig 中的 JavaTimeModule
     */
    private Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setTimeZone(TimeZone.getTimeZone(JacksonConfig.TIME_ZONE));

        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 使用 JacksonConfig 中配置的 JavaTimeModule
        objectMapper.registerModule(javaTimeModule);

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

}
