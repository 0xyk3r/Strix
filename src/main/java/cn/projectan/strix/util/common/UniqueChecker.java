package cn.projectan.strix.util.common;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.exception.StrixUniqueCheckerException;
import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.util.reflect.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库字段重复检测器
 *
 * @author ProjectAn
 * @since 2021/6/17 14:13
 */
@Slf4j
public class UniqueChecker {

    // 缓存: 实体类 → (分组 key → 字段集合)
    private static final ConcurrentHashMap<Class<?>, Map<String, Set<String>>> FIELD_CACHE = new ConcurrentHashMap<>();

    // 缓存: 实体类 → (分组 key → 提示名称)
    private static final ConcurrentHashMap<Class<?>, Map<String, String>> NAME_CACHE = new ConcurrentHashMap<>();

    // 缓存: 实体类 → IService
    private static final ConcurrentHashMap<Class<?>, IService<?>> SERVICE_CACHE = new ConcurrentHashMap<>();

    /**
     * 重复性检查工具
     * <p>注意：实体类必须有对应的 IService 实现在 Spring 容器中
     *
     * @param obj 需要检查的对象，需为数据库bean
     * @param <T> 对象类型
     */
    @SuppressWarnings("unchecked")
    public static <T> void check(T obj) {
        try {
            Class<?> clazz = obj.getClass();

            // 获取 ID, ID 存在有效值则代表修改，会自动根据 ID 排除自身
            String id = ReflectUtil.getString(obj, "id");

            // 从缓存获取字段元数据
            Map<String, Set<String>> groups = FIELD_CACHE.computeIfAbsent(clazz, k -> {
                Map<String, Set<String>> g = new HashMap<>();
                for (Field field : k.getDeclaredFields()) {
                    for (UniqueField annotation : field.getAnnotationsByType(UniqueField.class)) {
                        String groupKey = "group" + annotation.group();
                        g.computeIfAbsent(groupKey, s -> new HashSet<>()).add(field.getName());
                    }
                }
                return g;
            });

            if (groups.isEmpty()) {
                log.warn("UniqueDetectionTool: 对象 {} 未找到 @UniqueField 注解.", clazz.getName());
                return;
            }

            // 从缓存获取提示名称
            Map<String, String> names = NAME_CACHE.computeIfAbsent(clazz, k -> {
                Map<String, String> n = new HashMap<>();
                for (Field field : k.getDeclaredFields()) {
                    for (UniqueField annotation : field.getAnnotationsByType(UniqueField.class)) {
                        String groupKey = "group" + annotation.group();
                        n.merge(groupKey, annotation.value(), (oldVal, newVal) ->
                                oldVal + (annotation.group() == 0 ? I18nUtil.get("common.or") : I18nUtil.get("common.and")) + newVal);
                    }
                }
                return n;
            });

            // 从缓存获取 Service
            IService<T> service = findServiceCached((Class<T>) clazz);
            if (service == null) {
                log.error("UniqueDetectionTool: 未找到实体类 {} 对应的 IService 实现", clazz.getName());
                throw new StrixUniqueCheckerException(I18nUtil.get("error.unique.checkFailed") + " (e02)");
            }

            for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
                QueryWrapper<T> checkQueryWrapper = new QueryWrapper<>();
                if (StringUtils.hasText(id)) {
                    checkQueryWrapper.ne("id", id);
                }

                Set<String> fieldSet = group.getValue();
                if ("group0".equals(group.getKey())) {
                    checkQueryWrapper.and(qw -> {
                        for (String field : fieldSet) {
                            String value = ReflectUtil.getString(obj, field);
                            if (StringUtils.hasText(value)) {
                                qw.eq("`" + StrUtil.toUnderlineCase(field) + "`", value).or();
                            }
                        }
                    });
                } else {
                    for (String field : fieldSet) {
                        String value = ReflectUtil.getString(obj, field);
                        if (StringUtils.hasText(value)) {
                            checkQueryWrapper.eq('`' + StrUtil.toUnderlineCase(field) + '`', value);
                        }
                    }
                }

                if (service.count(checkQueryWrapper) > 0) {
                    String tips = names.get(group.getKey());
                    throw new StrixUniqueCheckerException(StringUtils.hasText(tips) ? tips + I18nUtil.get("error.unique.duplicateData") : I18nUtil.get("error.unique.checkNotPassed"));
                }
            }
        } catch (StrixUniqueCheckerException e) {
            throw e;
        } catch (Exception e) {
            log.error("UniqueChecker exception: {}", e.getMessage(), e);
            throw new StrixUniqueCheckerException(I18nUtil.get("error.unique.checkerError"));
        }
    }

    /**
     * 根据实体类查找对应的 IService 实现（带缓存）
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 对应的 IService 实现，如果未找到则返回 null
     */
    @SuppressWarnings("unchecked")
    private static <T> IService<T> findServiceCached(Class<T> entityClass) {
        return (IService<T>) SERVICE_CACHE.computeIfAbsent(entityClass, k -> {
            String serviceBeanName = StrUtil.lowerFirst(k.getSimpleName()) + "Service";
            Object bean = SpringUtil.getBean(serviceBeanName);

            if (bean instanceof IService) {
                ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(IService.class);
                if (resolvableType.getGeneric(0).resolve() == k) {
                    return (IService<?>) bean;
                }
            }
            return null;
        });
    }

}
