package cn.projectan.strix.model.annotation;

import java.lang.annotation.*;

/**
 * 可以匿名访问的接口
 *
 * @author 安炯奕
 * @date 2023/4/6 16:23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {

}
