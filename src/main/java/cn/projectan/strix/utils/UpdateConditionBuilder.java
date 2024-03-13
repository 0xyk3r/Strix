package cn.projectan.strix.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.annotation.UpdateField;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strix UpdateWrapper构造器
 *
 * @author ProjectAn
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
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        // 设置要修改的数据 ID
        String id = ReflectUtil.getString(bean, "id");
        if (!StringUtils.hasText(id)) {
            log.warn("构造UpdateCondition失败，无法获取原数据 ID");
            throw new StrixException("构造修改条件失败，原数据可能不存在");
        }
        updateWrapper.eq("id", id);

        // 设置修改用户
        String updateBy = SecurityUtils.getManagerId();
        if (StringUtils.hasText(updateBy)) {
            updateWrapper.set("update_by", updateBy);
        }

        AtomicInteger setCount = new AtomicInteger();

        Field[] fields = req.getClass().getDeclaredFields();
        for (Field field : fields) {
            UpdateField annotation = field.getAnnotation(UpdateField.class);
            if (annotation != null) {
                String newValue = ReflectUtil.getString(req, field.getName());
                String originalValue = ReflectUtil.getString(bean, field.getName());

                if (StringUtils.hasText(newValue) || annotation.allowEmpty()) {
                    if (!StringUtils.hasText(newValue) && annotation.allowEmpty()) {
                        newValue = "null".equals(annotation.defaultValue()) ? null : annotation.defaultValue();
                    }
                    // 仅当数据发生变动才执行 set 语句
                    if (!Objects.equals(originalValue, newValue)) {
                        updateWrapper = updateWrapper.set("`" + StrUtil.toUnderlineCase(field.getName()) + "`", newValue).or();
                        setCount.getAndIncrement();
                        // 转换回原数据类型，并回写数据
                        ReflectUtil.set(bean, field.getName(), Convert.convert(field.getType(), newValue));
                    }
                }
            }
        }
        Assert.isTrue(setCount.get() > 0, "未修改任何数据");
        return updateWrapper;
    }

}
