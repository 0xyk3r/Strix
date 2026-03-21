package cn.projectan.strix.core.xss;

import java.util.regex.Pattern;

/**
 * XSS 清理工具
 * <p>
 * 使用 HTML 标签剥离方式清理 XSS，而非实体转义。
 * 只移除真正的 HTML 标签和危险属性/协议，保留非标签的 &lt;&gt; 字符。
 * </p>
 * <p>
 * 例如 {@code <<<|||SEPARATOR|||>>>} 不会被影响，
 * 但 {@code <script>alert('XSS')</script>} 会被清理为 {@code alert('XSS')}。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class XssCleaner {

    /**
     * HTML 标签模式: &lt;tag ...&gt;, &lt;/tag&gt;, &lt;!-- ... --&gt;
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "</?[a-zA-Z][^>]*>|<!--[\\s\\S]*?-->",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    /**
     * 危险 URL 协议: javascript:, vbscript:, data: (带有可选空白)
     */
    private static final Pattern DANGEROUS_PROTOCOL_PATTERN = Pattern.compile(
            "(?:javascript|vbscript|data)\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * HTML 事件处理属性: onload=, onerror=, onclick= 等
     */
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "\\bon[a-z]+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 清理输入字符串中的 XSS 内容
     *
     * @param input 原始输入
     * @return 清理后的安全字符串
     */
    public static String clean(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        result = DANGEROUS_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll("");
        return result;
    }

}
