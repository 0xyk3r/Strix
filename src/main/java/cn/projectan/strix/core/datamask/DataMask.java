package cn.projectan.strix.core.datamask;

import java.lang.annotation.*;

/**
 * @author 安炯奕
 * @date 2023/2/22 14:28
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataMask {

    DataMaskFunc maskFunc() default DataMaskFunc.KEEP_NONE;

    char maskChar() default '*';

    int n1() default 2;

    int n2() default 2;

}
