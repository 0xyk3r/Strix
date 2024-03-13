package cn.projectan.strix.model.annotation;

import java.lang.annotation.*;

/**
 * @author ProjectAn
 * @date 2021/6/17 14:14
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(UniqueDetections.class)
public @interface UniqueDetection {

    /**
     * @return 代表分组 每个分组为独立查询 默认分组(0)以or连接 其他分组以and连接 可用于判断多个变量不能同时相同
     */
    int group() default 0;

    /**
     * @return 字段名称 用于提示 必须传入
     */
    String value();

}
