package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

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
     * @param clazz     类
     * @param fieldName 字段名
     * @return getter方法名
     */
    public static Method getGetter(Class<?> clazz, String fieldName) {
        try {
            String getterName = clazz.isRecord() ? fieldName : "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return clazz.getMethod(getterName);
        } catch (Exception e) {
            log.error("获取字段的getter方法失败", e);
            return null;
        }
    }

    /**
     * 获取字段的setter方法
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @return setter方法名
     */
    public static Method getSetter(Class<?> clazz, String fieldName) {
        try {
            String setterName = clazz.isRecord() ? fieldName : "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return clazz.getMethod(setterName, clazz.getDeclaredField(fieldName).getType());
        } catch (Exception e) {
            log.error("获取字段的setter方法失败", e);
            return null;
        }
    }

}
