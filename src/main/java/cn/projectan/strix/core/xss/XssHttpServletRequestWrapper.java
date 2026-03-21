package cn.projectan.strix.core.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS 安全请求包装器
 * <p>
 * 对 Query Parameter 应用 XssCleaner 清理，防止通过 URL 参数注入 XSS。
 * JSON 请求体的 XSS 清理由 {@link XssStringDeserializer} 在 Jackson 层处理。
 * </p>
 *
 * @author ProjectAn
 * @since 2025-03-21
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return XssCleaner.clean(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = XssCleaner.clean(values[i]);
        }
        return cleanValues;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> cleaned = new LinkedHashMap<>(original.size());
        original.forEach((key, values) -> {
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = XssCleaner.clean(values[i]);
            }
            cleaned.put(key, cleanValues);
        });
        return cleaned;
    }

}
