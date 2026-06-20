package cn.projectan.strix.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段在 JSON 反序列化时跳过 XSS 清洗。
 * <p>
 * 用于需要保留原始标签内容的字段（如 SSML 标记文本 {@code <speak>...</speak>}），
 * 由 {@link cn.projectan.strix.core.xss.XssStringDeserializer} 识别并放行。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XssIgnore {
}
