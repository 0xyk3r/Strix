package cn.projectan.strix.util;

import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.exception.StrixUniqueCheckerException;
import cn.projectan.strix.model.annotation.UniqueField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
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

    private static final String SERVICE_SUFFIX = "Service";

    /**
     * 重复性检查工具
     * <p>注意：仅支持在Controller或Service中调用
     * <p>注意：调用方不得包含超过1个与"controller"或"service"完全匹配的包路径
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
                log.warn("UniqueDetectionTool: {}未获取到Fields", clazz.getName());
                throw new StrixUniqueCheckerException("重复检查器配置异常");
            }
            // 有ID代表修改，根据ID排除自身
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

            Class<?> caller = CallerUtil.getCallerCaller();
            String callerName = caller.getName();
            String callerPackageName;
            // FIXME 如果用户包名完全匹配controller或service，这里的判断会有问题，不过几乎不存在此情况
            if (callerName.contains(".controller.")) {
                callerPackageName = callerName.substring(0, callerName.indexOf(".controller.")) + ".service.";
            } else if (callerName.contains(".service.")) {
                callerPackageName = callerName.substring(0, callerName.indexOf(".service.")) + ".service.";
            } else {
                log.error("UniqueDetectionTool: 调用方{}不是Controller或Service，不支持调用", callerName);
                throw new StrixUniqueCheckerException("重复检查器配置异常");
            }
            Class<?> serviceClazz = Class.forName(callerPackageName + clazz.getSimpleName() + SERVICE_SUFFIX);
            IService<T> service = (IService<T>) SpringUtil.getBean(serviceClazz);

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

}
