package cn.projectan.strix.utils;

import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.exception.StrixUniqueDetectionException;
import cn.projectan.strix.model.annotation.UniqueDetection;
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
 * 数据库字段重复检测工具
 *
 * @author 安炯奕
 * @date 2021/6/17 14:13
 */
@Slf4j
public class UniqueDetectionTool {

    private static final String SERVICE_SUFFIX = "Service";

    /**
     * 重复性检查工具 <br>
     * 注意：仅支持在Controller或Service中调用 <br>
     * 注意：调用方不得包含超过1个与"controller"或"service"完全匹配的包路径 <br>
     *
     * @param obj 需要检查的对象，需为数据库bean
     * @param <T> 对象类型
     */
    public static <T> void check(T obj) {
        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            if (fields.length == 0) {
                log.warn("UniqueDetectionTool: " + clazz.getName() + "未获取到Fields");
                throw new StrixUniqueDetectionException("重复检查器配置异常");
            }
            // 有ID代表修改，根据ID排除自身
            // FIXME 这里由于 ReflectUtil 内部异常捕获机制，如果 ID 字段或 getId 方法不存在，会导致逻辑错误
            String id = ReflectUtil.getString(obj, "id");
            // 遍历需要重复检查的字段
            Map<String, Set<String>> groups = new HashMap<>();
            Map<String, String> names = new HashMap<>();
            for (Field field : fields) {
                UniqueDetection[] annotationsByType = field.getAnnotationsByType(UniqueDetection.class);
                for (UniqueDetection annotation : annotationsByType) {
                    int groupId = annotation.group();
                    String name = annotation.value();
                    Set<String> group = groups.get("group" + groupId);
                    if (group == null) {
                        group = new HashSet<>();
                    }
                    group.add(field.getName());
                    groups.put("group" + groupId, group);
                    String existNames = names.get("group" + groupId);
                    if (StringUtils.hasText(existNames)) {
                        names.put("group" + groupId, existNames + (groupId == 0 ? "或" : "和") + name);
                    } else {
                        names.put("group" + groupId, name);
                    }
                }
            }

            Class<?> caller = CallerUtil.getCallerCaller();
            String callerName = caller.getName();
            String callerPackageName;
            // FIXME 如果用户包名完全匹配controller或service，这里的判断会有问题，不过几乎不可能
            if (callerName.contains(".controller.")) {
                callerPackageName = callerName.substring(0, callerName.indexOf(".controller.")) + ".service.";
            } else if (callerName.contains(".service.")) {
                callerPackageName = callerName.substring(0, callerName.indexOf(".service.")) + ".service.";
            } else {
                log.error("UniqueDetectionTool: 调用方" + callerName + "不是Controller或Service，不支持调用");
                throw new StrixUniqueDetectionException("重复检查器配置异常");
            }
            Class<?> serviceClazz = Class.forName(callerPackageName + clazz.getSimpleName() + SERVICE_SUFFIX);
            IService<T> service = (IService<T>) SpringUtil.getBean(serviceClazz);

            for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
                if ("group0".equals(group.getKey())) {
                    QueryWrapper<T> checkQueryWrapper = new QueryWrapper<>();
                    if (StringUtils.hasText(id)) {
                        checkQueryWrapper.ne("id", id);
                    }
                    Set<String> fieldSet = group.getValue();
                    checkQueryWrapper.and(qw -> {
                        try {
                            for (String field : fieldSet) {
                                String value = ReflectUtil.getString(obj, field);
                                if (StringUtils.hasText(value)) {
                                    qw.eq("`" + StrUtil.toUnderlineCase(field) + "`", value).or();
                                }
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            throw new StrixUniqueDetectionException("重复检查器工作异常");
                        }
                    });
                    if (service.count(checkQueryWrapper) > 0) {
                        String tips = names.get(group.getKey());
                        if (StringUtils.hasText(tips)) {
                            throw new StrixUniqueDetectionException(tips + "与系统内已有数据重复");
                        } else {
                            throw new StrixUniqueDetectionException("数据重复检查不通过");
                        }
                    }
                } else {
                    QueryWrapper<T> checkQueryWrapper = new QueryWrapper<>();
                    if (StringUtils.hasText(id)) {
                        checkQueryWrapper.ne("id", id);
                    }
                    Set<String> fieldSet = group.getValue();
                    for (String field : fieldSet) {
                        String value = ReflectUtil.getString(obj, field);
                        if (StringUtils.hasText(value)) {
                            checkQueryWrapper.eq('`' + StrUtil.toUnderlineCase(field) + '`', value);
                        }
                    }
                    if (service.count(checkQueryWrapper) > 0) {
                        String tips = names.get(group.getKey());
                        if (StringUtils.hasText(tips)) {
                            throw new StrixUniqueDetectionException(tips + "同时与系统内已有数据重复");
                        } else {
                            throw new StrixUniqueDetectionException("数据重复检查不通过");
                        }
                    }
                }
            }

        } catch (StrixUniqueDetectionException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new StrixUniqueDetectionException("重复检查器工作异常");
        }
    }

}
