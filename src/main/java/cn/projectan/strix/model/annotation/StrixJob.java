package cn.projectan.strix.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为了保证系统安全，Strix 要求所有的 Job 都必须使用 @StrixJob 注解，否则无法创建任务并执行。
 *
 * @author ProjectAn
 * @date 2023/8/1 15:15
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrixJob {
}
