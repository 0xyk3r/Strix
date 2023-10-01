package cn.projectan.strix.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * @author 安炯奕
 * @date 2023/6/17 14:36
 */
public class ServletUtils {

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes sra) {
            return sra;
        }
        return null;
    }

    /**
     * 获取 request
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getRequestAttributes();
        Assert.notNull(attributes, "request attributes is null");
        return attributes.getRequest();
    }

    /**
     * 获取 response
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getRequestAttributes();
        Assert.notNull(attributes, "request attributes is null");
        return attributes.getResponse();
    }

    /**
     * 将 QueryString 转换为 Map
     *
     * @param request HttpServletRequest
     * @return Map
     */
    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return OkHttpUtil.parseQueryParamToMap(queryString);
    }

}
