package cn.projectan.strix.model.annotation;

import java.lang.annotation.*;

/**
 * 数据库字段加密注解
 * <p>
 * 标记在实体类字段上，该字段在存入数据库时自动加密，从数据库读取时自动解密。
 * <p>
 * 注意事项：
 * <ul>
 *   <li>仅支持 String 类型字段</li>
 *   <li>加密后数据长度会增加，请确保数据库字段长度足够</li>
 *   <li>兼容 MyBatis-Plus 的各种查询和更新方式</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026/01/29 02:10
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {

}
