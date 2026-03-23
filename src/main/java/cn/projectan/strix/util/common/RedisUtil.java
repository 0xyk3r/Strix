package cn.projectan.strix.util.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * <p>提供对 Redis 各种数据结构的操作封装，包括：
 * <ul>
 *     <li>String: 基本的键值操作</li>
 *     <li>Hash: 哈希表操作</li>
 *     <li>Set: 集合操作</li>
 *     <li>List: 列表操作</li>
 *     <li>ZSet: 有序集合操作</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2021/05/12 19:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    /**
     * 默认时间单位：秒
     */
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 默认 scan 批次大小
     */
    private static final int DEFAULT_SCAN_COUNT = 1000;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存失效时间
     *
     * @param key 键
     * @return 时间(秒) 返回0代表永久有效，-2代表键不存在
     */
    public long getExpire(String key) {
        return getExpire(key, DEFAULT_TIME_UNIT);
    }

    /**
     * 获取缓存失效时间
     *
     * @param key      键
     * @param timeUnit 时间单位
     * @return 失效时间，返回0代表永久有效，-2代表键不存在
     */
    public long getExpire(String key, TimeUnit timeUnit) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Long expire = redisTemplate.getExpire(key, timeUnit);
        return expire != null ? expire : -2;
    }

    /**
     * 设置缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return 是否设置成功
     */
    public boolean setExpire(String key, long time) {
        return setExpire(key, time, DEFAULT_TIME_UNIT);
    }

    /**
     * 设置缓存失效时间
     *
     * @param key      键
     * @param time     时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    public boolean setExpire(String key, long time, TimeUnit timeUnit) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Assert.isTrue(time > 0, I18nUtil.get("assert.redis.cacheExpirePositive"));
        Boolean result = redisTemplate.expire(key, time, timeUnit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 判断 Key 是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 判断 Key 是否是指定类型
     *
     * @param key  键
     * @param type 类型 {@link DataType}
     * @return 是否是指定类型
     */
    public boolean isType(String key, DataType type) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Assert.notNull(type, I18nUtil.notEmpty("field.redisType"));
        return redisTemplate.type(key) == type;
    }

    /**
     * 删除 Key
     *
     * @param keys 键，可以传一个值或多个
     */
    public void del(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        if (keys.length == 1) {
            redisTemplate.delete(keys[0]);
        } else {
            redisTemplate.delete(Arrays.asList(keys));
        }
    }

    /**
     * 模糊查询 Keys
     * <p>大数据量场景请使用 {@link #scan(String)}，避免阻塞 Redis
     *
     * @param pattern 匹配模式（支持通配符 *）
     * @return 匹配的 Keys 集合
     */
    public Set<String> keys(String pattern) {
        Assert.hasText(pattern, I18nUtil.notEmpty("field.redisPattern"));
        return redisTemplate.keys(pattern);
    }

    /**
     * 模糊删除 Keys
     * <p>使用 scan 命令分批处理，避免阻塞 Redis
     *
     * @param pattern 匹配模式（需要包含通配符 *）
     */
    public void delLike(String pattern) {
        Assert.hasText(pattern, I18nUtil.notEmpty("field.redisPattern"));
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(pattern)
                        .count(DEFAULT_SCAN_COUNT)
                        .build()
        )) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.unlink(keys);
            }
        }
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 使用管道批量获取多个键的值
     *
     * @param keys 键列表
     * @return 值列表，顺序与keys一致
     */
    public List<Object> pipelineGet(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }

        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.stringCommands().get(key.getBytes());
            }
            return null;
        });
    }

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Assert.hasText(value.toString(), "value 不能为空");
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     */
    public void set(String key, Object value, long time) {
        set(key, value, time, DEFAULT_TIME_UNIT);
    }

    /**
     * 设置缓存
     *
     * @param key      键
     * @param value    值
     * @param time     时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        Assert.hasText(key, I18nUtil.notEmpty("field.redisKey"));
        Assert.hasText(value.toString(), "value 不能为空");
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, timeUnit);
        } else {
            set(key, value);
        }
    }

    /**
     * 使用管道批量设置多个键值对
     *
     * @param keyValueMap 键值对映射
     */
    public void pipelineSet(Map<String, Object> keyValueMap) {
        if (CollectionUtils.isEmpty(keyValueMap)) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keyValueMap.forEach((key, value) -> {
                byte[] keyBytes = key.getBytes();
                try {
                    // 使用原始字节序列化
                    @SuppressWarnings("unchecked")
                    byte[] valueBytes = value != null ?
                            ((RedisSerializer<Object>) redisTemplate.getValueSerializer()).serialize(value) :
                            null;

                    if (valueBytes != null) {
                        connection.stringCommands().set(keyBytes, valueBytes);
                    }
                } catch (Exception e) {
                    log.error("序列化键值对失败: key={}", key, e);
                }
            });
            return null;
        });
    }

    /**
     * 递增
     *
     * @param key 键
     * @return 递增后的值
     */
    public long incr(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递增失败", e);
            return 0;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 递增后的值
     */
    public long incr(String key, long delta) {
        Assert.isTrue(delta > 0, I18nUtil.get("assert.redis.incrementPositive"));
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递增失败", e);
            return 0;
        }
    }

    /**
     * 递减
     *
     * @param key 键
     * @return 递减后的值
     */
    public long decr(String key) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递减失败", e);
            return 0;
        }
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return 递减后的值
     */
    public long decr(String key, long delta) {
        Assert.isTrue(delta > 0, I18nUtil.get("assert.redis.decrementPositive"));
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递减失败", e);
            return 0;
        }
    }

    /**
     * 获取指定Hash表的值
     *
     * @param key  Hash表键
     * @param item 项
     * @return 值
     */
    public Object hGet(String key, String item) {
        try {
            return redisTemplate.opsForHash().get(key, item);
        } catch (Exception e) {
            log.error("获取指定Hash表的值失败", e);
            return null;
        }
    }

    /**
     * 获取Hash表中包含的所有键值对
     *
     * @param key Hash表键
     * @return Hash表中包含的所有键值对
     */
    public Map<Object, Object> hEntries(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("获取Hash表中包含的所有键值对失败", e);
            return null;
        }
    }

    /**
     * 添加多个键值对到Hash表键中
     *
     * @param key Hash表键
     * @param map 键值对
     */
    public void hMSet(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
        } catch (Exception e) {
            log.error("添加多个键值对到Hash表键中失败", e);
        }
    }

    /**
     * 添加键值对到Hash表中
     *
     * @param key   Hash表键
     * @param item  项
     * @param value 值
     */
    public void hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
        } catch (Exception e) {
            log.error("添加键值对到Hash表中失败", e);
        }
    }

    /**
     * 删除Hash表中的项
     *
     * @param key  Hash表键
     * @param item 项
     */
    public void hDel(String key, Object... item) {
        try {
            redisTemplate.opsForHash().delete(key, item);
        } catch (Exception e) {
            log.error("删除Hash表中的项失败", e);
        }
    }

    /**
     * 判断Hash表中是否存在该项
     *
     * @param key  Hash表键
     * @param item 项
     * @return 是否存在
     */
    public boolean hHasKey(String key, String item) {
        try {
            return redisTemplate.opsForHash().hasKey(key, item);
        } catch (Exception e) {
            log.error("判断Hash表中是否存在该项失败", e);
            return false;
        }
    }

    /**
     * Hash表项递增
     *
     * @param key   键
     * @param item  项
     * @param delta 递增因子
     * @return 递增后的值
     */
    public long hIncr(String key, String item, long delta) {
        try {
            return redisTemplate.opsForHash().increment(key, item, delta);
        } catch (Exception e) {
            log.error("Hash表项递增失败", e);
            return 0;
        }
    }

    /**
     * Hash表项递减
     *
     * @param key   键
     * @param item  项
     * @param delta 递减因子
     * @return 递减后的值
     */
    public long hDecr(String key, String item, long delta) {
        try {
            return redisTemplate.opsForHash().increment(key, item, -delta);
        } catch (Exception e) {
            log.error("Hash表项递减失败", e);
            return 0;
        }
    }

    /**
     * 获取 Set 中的所有内容
     *
     * @param key 键
     * @return 值
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取Set中的所有内容失败", e);
            return null;
        }
    }

    /**
     * 判断 Set 中是否存在某个值
     *
     * @param key   键
     * @param value 值
     * @return 是否存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
            return result != null ? result : false;
        } catch (Exception e) {
            log.error("判断Set中是否存在某个值失败", e);
            return false;
        }
    }

    /**
     * 向 Set 中放入数据
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("向Set中放入数据失败", e);
            return 0;
        }
    }

    /**
     * 获取 Set 缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long sSize(String key) {
        try {
            Long result = redisTemplate.opsForSet().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Set缓存的长度失败", e);
            return 0;
        }
    }

    /**
     * 批量获取多个Set的大小
     *
     * @param keys Set键集合
     * @return 键与大小的映射
     */
    public Map<String, Long> sMultiSize(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new HashMap<>(keys.size());

        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(@NotNull RedisOperations operations) {
                for (String key : keys) {
                    operations.opsForSet().size(key);
                }
                return null;
            }
        }).forEach(size -> {
            if (size instanceof Long) {
                String key = new ArrayList<>(keys).get(result.size());
                result.put(key, (Long) size);
            }
        });

        return result;
    }

    /**
     * 移除 Set 缓存中的值
     *
     * @param key    键
     * @param values 值
     * @return 移除的个数
     */
    public long sRemove(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("移除Set缓存中的值失败", e);
            return 0;
        }
    }

    /**
     * 通过索引获取 List 中指定的元素
     * <p>索引从零开始, 0表示第一个元素, 1表示第二个元素</p>
     * <p>负索引可用于指定从列表尾部开始的元素, -1表示最后一个元素, -2表示倒数第二个</p>
     *
     * @param key   键
     * @param index 索引
     * @return 值
     */
    public Object lGet(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("通过索引获取 List 中指定的元素失败", e);
            return null;
        }
    }

    /**
     * 获取 List 在给定索引范围上的所有值
     * <p>索引从零开始, 0表示第一个元素, 1表示第二个元素</p>
     * <p>负索引可用于指定从列表尾部开始的元素, -1表示最后一个元素, -2表示倒数第二个</p>
     *
     * @param key   键
     * @param start 索引范围开始
     * @param end   索引范围结束
     * @return 值
     */
    public List<Object> lGetRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取 List 在给定索引范围上的所有值失败", e);
            return null;
        }
    }

    /**
     * 获取 List 缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long lSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取 List 缓存的长度失败", e);
            return 0;
        }
    }

    /**
     * 向 List 中放入数据
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lRightPush(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("向 List 中放入数据失败", e);
            return false;
        }
    }

    /**
     * 向 List 中放入数据
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lLeftPush(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("向 List 中放入数据失败", e);
            return false;
        }
    }

    /**
     * 向 List 中放入多个数据
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lRightPush(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("向 List 中放入数据失败", e);
            return false;
        }
    }

    /**
     * 向 List 中放入多个数据
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lLeftPush(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().leftPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("向 List 中放入数据失败", e);
            return false;
        }
    }

    /**
     * 向 List 中的指定位置放入数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("向 List 中的指定位置放入数据失败", e);
            return false;
        }
    }

    /**
     * 移除 List 中的数据
     * <br>根据参数 COUNT 的值，移除列表中与参数 VALUE 相等的元素。
     * <br>count > 0 : 从表头开始向表尾搜索，移除与 VALUE 相等的元素，数量为 COUNT 。
     * <br>count < 0 : 从表尾开始向表头搜索，移除与 VALUE 相等的元素，数量为 COUNT 的绝对值。
     * <br>count = 0 : 移除表中所有与 VALUE 相等的值。
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 被移除元素数量
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long result = redisTemplate.opsForList().remove(key, count, value);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("移除List中的某条数据失败", e);
            return 0;
        }
    }

    /**
     * 从有序集合中获取指定元素的分数
     *
     * @param key  键
     * @param item 元素
     * @return 分数，如果元素不存在返回 null
     */
    public Long zGet(String key, String item) {
        Double result = redisTemplate.opsForZSet().score(key, item);
        return result != null ? result.longValue() : null;
    }

    /**
     * 获取有序集合中的所有元素及其分数
     *
     * @param key 键
     * @return 元素及分数的集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zGet(String key) {
        return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
    }

    /**
     * 向有序集合中添加元素
     *
     * @param key   键
     * @param item  元素
     * @param score 分数
     */
    public void zSet(String key, String item, long score) {
        redisTemplate.opsForZSet().add(key, item, score);
    }

    /**
     * 批量向有序集合中添加元素
     *
     * @param key    键
     * @param tuples 元素及分数的集合
     */
    public void zSet(String key, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 有序集合元素分数递增 1
     *
     * @param key  键
     * @param item 元素
     */
    public void zIncr(String key, String item) {
        redisTemplate.opsForZSet().incrementScore(key, item, 1);
    }

    /**
     * 从有序集合中删除元素
     *
     * @param key  键
     * @param item 元素
     */
    public void zDel(String key, String item) {
        redisTemplate.opsForZSet().remove(key, item);
    }

    /**
     * 使用 scan 命令扫描匹配的 Keys
     * <p>相比 keys 命令，scan 不会阻塞 Redis，适用于大数据量场景
     *
     * @param pattern 匹配模式（支持通配符 *）
     * @return 匹配的 Keys 集合
     */
    public Set<String> scan(String pattern) {
        return scan(pattern, DEFAULT_SCAN_COUNT);
    }

    /**
     * 使用 scan 命令扫描匹配的 Keys
     * <p>相比 keys 命令，scan 不会阻塞 Redis，适用于大数据量场景
     *
     * @param pattern 匹配模式（支持通配符 *）
     * @param count   每批扫描的数量
     * @return 匹配的 Keys 集合
     */
    public Set<String> scan(String pattern, long count) {
        Assert.hasText(pattern, I18nUtil.notEmpty("field.redisPattern"));
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(pattern)
                        .count(count)
                        .build()
        )) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }

    /**
     * ping
     */
    public void ping() {
        redisTemplate.execute(RedisConnectionCommands::ping);
    }

}

