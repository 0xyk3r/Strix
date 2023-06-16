package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * 反射工具类
 *
 * @author 安炯奕
 * @date 2023/5/23 12:50
 */
@Slf4j
public class ReflectUtil {

    /**
     * 获取字段的getter方法
     *
     * @param clazz 类
     * @param field 字段名
     * @return getter 方法名
     */
    public static Method getter(Class<?> clazz, String field) {
        try {
            String getterName = clazz.isRecord() ? field : "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
            return clazz.getMethod(getterName);
        } catch (Exception e) {
            log.warn("ReflectUtil: 获取字段的getter失败", e);
            return null;
        }
    }

    /**
     * 获取字段的setter方法
     *
     * @param clazz 类
     * @param field 字段名
     * @return setter 方法名
     */
    public static Method setter(Class<?> clazz, String field) {
        try {
            String setterName = clazz.isRecord() ? field : "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
            return clazz.getMethod(setterName, clazz.getDeclaredField(field).getType());
        } catch (Exception e) {
            log.warn("ReflectUtil: 获取字段的setter失败", e);
            return null;
        }
    }

    public static Object get(Object obj, String field) {
        try {
            Class<?> clazz = obj.getClass();
            Method getter = ReflectUtil.getter(clazz, field);
            if (getter != null) {
                return getter.invoke(obj);
            }
        } catch (Exception e) {
            log.warn("ReflectUtil: 反射调用getter失败", e);
        }
        return null;
    }

    public static String getString(Object obj, String field) {
        return Optional.ofNullable(get(obj, field)).map(Objects::toString).orElse(null);
    }

    public static Integer getInteger(Object obj, String field) {
        return Optional.ofNullable(getString(obj, field)).map(Integer::parseInt).orElse(null);
    }

    public static void set(Object obj, String field, Object value) {
        try {
            Class<?> clazz = obj.getClass();
            Method setter = ReflectUtil.setter(clazz, field);
            if (setter != null) {
                setter.invoke(obj, value);
            }
        } catch (Exception e) {
            log.warn("ReflectUtil: 反射调用setter失败", e);
        }
    }

}
