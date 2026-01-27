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

/**
 * 数据库字段重复检测器
 *
 * @author ProjectAn
 * @since 2021/6/17 14:13
 */
@Slf4j
public class UniqueChecker {

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
            Field[] fields = clazz.getDeclaredFields();
            if (fields.length == 0) {
                log.warn("UniqueDetectionTool: 对象 {} 未获取到 Fields.", clazz.getName());
                throw new StrixUniqueCheckerException("数据重复检查失败 (e01)");
            }

            // 获取 ID, ID 存在有效值则代表修改，会自动根据 ID 排除自身
            // 注意: 这里由于 ReflectUtil 内部异常捕获机制，如果 ID 字段或 getId 方法不存在，会导致逻辑错误
            String id = ReflectUtil.getString(obj, "id");

            // 遍历需要重复检查的字段
            Map<String, Set<String>> groups = new HashMap<>();
            Map<String, String> names = new HashMap<>();
            for (Field field : fields) {
                UniqueField[] annotationsByType = field.getAnnotationsByType(UniqueField.class);
                for (UniqueField annotation : annotationsByType) {
                    String groupKey = "group" + annotation.group();
                    groups.computeIfAbsent(groupKey, k -> new HashSet<>()).add(field.getName());
                    names.merge(groupKey, annotation.value(), (oldVal, newVal) -> oldVal + (annotation.group() == 0 ? "或" : "和") + newVal);
                }
            }

            // 通过实体类从 Spring 容器中查找管理该实体类型的 IService 服务类
            IService<T> service = findServiceForEntityClass((Class<T>) clazz);
            if (service == null) {
                log.error("UniqueDetectionTool: 未找到实体类 {} 对应的 IService 实现", clazz.getName());
                throw new StrixUniqueCheckerException("数据重复检查失败 (e02)");
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
                    throw new StrixUniqueCheckerException(StringUtils.hasText(tips) ? tips + "与系统内已有数据重复" : "数据重复检查不通过");
                }
            }
        } catch (StrixUniqueCheckerException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixUniqueCheckerException("重复检查器工作异常");
        }
    }

    /**
     * 根据实体类查找对应的 IService 实现
     * <p>通过解析 Spring 容器中所有 IService Bean 的泛型参数来匹配
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 对应的 IService 实现，如果未找到则返回 null
     */
    @SuppressWarnings("unchecked")
    private static <T> IService<T> findServiceForEntityClass(Class<T> entityClass) {
        String serviceBeanName = StrUtil.lowerFirst(entityClass.getSimpleName()) + "Service";
        Object bean = SpringUtil.getBean(serviceBeanName);

        if (bean instanceof IService) {
            ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(IService.class);
            if (resolvableType.getGeneric(0).resolve() == entityClass) {
                return (IService<T>) bean;
            }
        }

        return null;
    }

}
