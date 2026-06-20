package cn.projectan.strix.core.xss;


import cn.projectan.strix.model.annotation.XssIgnore;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Jackson 自定义 String 反序列化器
 * <p>
 * 在 JSON 反序列化时自动对所有 String 字段进行 XSS 清理。
 * 使用 HTML 标签剥离方式，不影响非 HTML 的特殊字符。
 * </p>
 * <p>
 * 实现 contextual 解析：字段标注 {@link XssIgnore} 时跳过清洗，返回原始字符串
 * （用于 SSML 标记等需要保留标签的字段）。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class XssStringDeserializer extends StdDeserializer<String> {

    /**
     * 是否跳过 XSS 清洗（由字段 {@link XssIgnore} 注解决定）
     */
    private final boolean ignore;

    public XssStringDeserializer() {
        this(false);
    }

    public XssStringDeserializer(boolean ignore) {
        super(String.class);
        this.ignore = ignore;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext context) {
        String value = p.getValueAsString();
        return ignore ? value : XssCleaner.clean(value);
    }

    /**
     * 按目标字段是否标注 {@link XssIgnore} 返回对应的解析器实例。
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property != null && property.getAnnotation(XssIgnore.class) != null) {
            return new XssStringDeserializer(true);
        }
        return this;
    }

}
