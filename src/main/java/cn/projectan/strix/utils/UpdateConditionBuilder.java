package cn.projectan.strix.utils;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.model.annotation.UpdateField;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strix UpdateWrapper构造器
 *
 * @author 安炯奕
 * @date 2021/6/17 17:20
 */
@Slf4j
public class UpdateConditionBuilder {

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
            String id = ReflectUtil.getString(bean, "id");
            if (!StringUtils.hasText(id)) {
                log.warn("无法获取原数据 ID");
                return null;
            }
            updateWrapper.eq("id", id);

            // 设置修改用户
            if (StringUtils.hasText(updateBy)) {
                updateWrapper.set("update_by", updateBy);
            }

            AtomicInteger setCount = new AtomicInteger();

            Field[] fields = reqClazz.getDeclaredFields();
            for (Field field : fields) {
                UpdateField annotation = field.getAnnotation(UpdateField.class);
                if (annotation != null) {
                    String newValue = ReflectUtil.getString(req, field.getName());
                    String originalValue = ReflectUtil.getString(bean, field.getName());

                    if (annotation.allowEmpty() || (StringUtils.hasText(newValue))) {
                        // 仅当数据发生变动才执行set语句
                        if ((originalValue == null && newValue != null) || (originalValue != null && !originalValue.equals(newValue))) {
                            updateWrapper = updateWrapper.set("`" + StrUtil.toUnderlineCase(field.getName()) + "`", newValue).or();
                            setCount.getAndIncrement();
                            // 回写数据
                            ReflectUtil.set(bean, field.getName(), newValue);
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
