package cn.projectan.strix.core.validation;

import jakarta.validation.MessageInterpolator;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;
import java.util.Map;

/**
 * 自定义消息插值器，支持 "{模板key:字段标签key}" 格式的参数化消息。
 * <p>
 * 示例：{@code message = "{validation.required:field.manager.nickname}"}
 * <ul>
 *   <li>解析模板 key: {@code validation.required} → "{0}不可为空"</li>
 *   <li>解析字段标签 key: {@code field.manager.nickname} → "管理人员昵称"</li>
 *   <li>替换 {0} → "管理人员昵称不可为空"</li>
 *   <li>替换注解属性 {min}, {max}, {value} 等</li>
 * </ul>
 *
 * @author ProjectAn
 */
public class StrixMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator delegate;
    private final MessageSource messageSource;

    public StrixMessageInterpolator(MessageInterpolator delegate, MessageSource messageSource) {
        this.delegate = delegate;
        this.messageSource = messageSource;
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return interpolate(messageTemplate, context, Locale.getDefault());
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        if (messageTemplate != null
                && messageTemplate.startsWith("{")
                && messageTemplate.endsWith("}")
                && messageTemplate.contains(":")) {

            String inner = messageTemplate.substring(1, messageTemplate.length() - 1);
            int colonIndex = inner.indexOf(':');

            if (colonIndex > 0) {
                String templateKey = inner.substring(0, colonIndex);
                String fieldLabelKey = inner.substring(colonIndex + 1);

                // 1. 解析字段标签
                String fieldLabel = resolveMessage(fieldLabelKey, locale, fieldLabelKey);

                // 2. 解析模板（含 {0} 占位符）
                String template = resolveMessage(templateKey, locale, templateKey);

                // 3. 替换 {0} 为字段标签
                String result = template.replace("{0}", fieldLabel);

                // 4. 替换注解属性（{min}, {max}, {value} 等）
                Map<String, Object> attrs = context.getConstraintDescriptor().getAttributes();
                for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                    String placeholder = "{" + entry.getKey() + "}";
                    if (result.contains(placeholder)) {
                        result = result.replace(placeholder, String.valueOf(entry.getValue()));
                    }
                }

                return result;
            }
        }

        // 不含冒号的标准格式，委托给默认 Interpolator
        return delegate.interpolate(messageTemplate, context, locale);
    }

    private String resolveMessage(String key, Locale locale, String defaultValue) {
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            return defaultValue;
        }
    }
}
