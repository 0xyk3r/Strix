package cn.projectan.strix.utils;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.model.annotation.UpdateField;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strix UpdateWrapper构造器
 *
 * @author 安炯奕
 * @date 2021/6/17 17:20
 */
@Slf4j
public class UpdateConditionBuilder {

    private static final char UNDERLINE = '_';

    private static final String GETTER_PREFIX = "get";

    private static final String SETTER_PREFIX = "set";

    private static final String UPDATE_BY_SETTER = "setUpdateBy";

    /**
     * 构造UpdateWrapper
     *
     * @param bean 原数据对象 (必须为刚从数据库查询得出，不得经过任何修改)
     * @param req  修改请求体 (必须包含带@UpdateField注解的字段)
     * @param <T>  数据对象类型
     * @param <V>  请求体类型
     * @return 用于执行update操作的UpdateWrapper
     */
    public static <T, V> UpdateWrapper<T> build(T bean, V req) {
        return build(bean, req, null);
    }

    /**
     * 构造UpdateWrapper
     *
     * @param bean     原数据对象 (必须为刚从数据库查询得出，不得经过任何修改)
     * @param req      修改请求体 (必须包含带@UpdateField注解的字段)
     * @param <T>      数据对象类型
     * @param <V>      请求体类型
     * @param updateBy 数据修改人
     * @return 用于执行update操作的UpdateWrapper
     */
    public static <T, V> UpdateWrapper<T> build(T bean, V req, String updateBy) {
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        try {
            Class<?> beanClazz = bean.getClass();
            Class<?> reqClazz = req.getClass();

            // 先设置要修改的数据id
            Method idGetter = beanClazz.getMethod("getId");
            Object idGetterInvoke = idGetter.invoke(bean);
            if (idGetterInvoke != null && StringUtils.hasText(idGetterInvoke.toString())) {
                updateWrapper.eq("id", idGetterInvoke.toString());
            } else {
                return null;
            }

            // 设置修改用户
            if (StringUtils.hasText(updateBy)) {
                updateWrapper.set("update_by", updateBy);
            }

            AtomicInteger setCount = new AtomicInteger();

            Field[] fields = reqClazz.getDeclaredFields();
            for (Field field : fields) {
                UpdateField annotation = field.getAnnotation(UpdateField.class);
                if (annotation != null) {
                    Method reqGetter = reqClazz.getMethod(GETTER_PREFIX + StrUtil.upperFirst(field.getName()));
                    Object reqGetterInvoke = reqGetter.invoke(req);
                    Method originalFieldGetter = beanClazz.getMethod(GETTER_PREFIX + StrUtil.upperFirst(field.getName()));
                    Object originalFieldGetterInvoke = originalFieldGetter.invoke(bean);
                    if (annotation.allowEmpty() || (reqGetterInvoke != null && StringUtils.hasText(reqGetterInvoke.toString()))) {
                        String newValue = reqGetterInvoke.toString();

                        // 仅当数据发生变动才执行set语句
                        if (originalFieldGetterInvoke == null || !originalFieldGetterInvoke.toString().equals(newValue)) {
                            updateWrapper = updateWrapper.set(StrUtil.toUnderlineCase(field.getName()), newValue).or();
                            setCount.getAndIncrement();
                            Method newFieldSetter = beanClazz.getMethod(SETTER_PREFIX + StrUtil.upperFirst(field.getName()), field.getType());
                            newFieldSetter.invoke(bean, reqGetterInvoke);
                        }
                    }
                }
            }
            Assert.isTrue(setCount.get() > 0, "未修改任何数据");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return updateWrapper;
    }

}
