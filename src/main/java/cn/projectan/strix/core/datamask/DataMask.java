package cn.projectan.strix.core.datamask;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 *
 * @author ProjectAn
 * @since 2023/2/22 14:28
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataMask {

    /**
     * 脱敏函数
     *
     * @see DataMaskFunc
     */
    DataMaskFunc maskFunc() default DataMaskFunc.KEEP_NONE;

    /**
     * 脱敏替换字符
     */
    char maskChar() default '*';

    /**
     * 脱敏参数1
     */
    int n1() default 2;

    /**
     * 脱敏参数2
     */
    int n2() default 2;

}
