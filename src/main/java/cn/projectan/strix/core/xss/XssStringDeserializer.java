package cn.projectan.strix.core.xss;


import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Jackson 自定义 String 反序列化器
 * <p>
 * 在 JSON 反序列化时自动对所有 String 字段进行 XSS 清理。
 * 使用 HTML 标签剥离方式，不影响非 HTML 的特殊字符。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class XssStringDeserializer extends StdDeserializer<String> {

    public XssStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext context) {
        String value = p.getValueAsString();
        return XssCleaner.clean(value);
    }

}
