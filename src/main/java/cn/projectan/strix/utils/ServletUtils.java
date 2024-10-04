package cn.projectan.strix.utils;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.io.IORuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Servlet 工具类
 *
 * @author ProjectAn
 * @since 2023/6/17 14:36
 */
public class ServletUtils {

    /**
     * 获取 RequestAttributes
     */
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

    /**
     * 获得 PrintWriter
     *
     * @param response 响应对象{@link HttpServletResponse}
     * @return 获得PrintWriter
     * @throws IORuntimeException IO异常
     */
    public static PrintWriter getWriter(HttpServletResponse response) throws IORuntimeException {
        try {
            return response.getWriter();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * 返回数据给客户端
     *
     * @param response 响应对象{@link HttpServletResponse}
     * @param text     返回的内容
     */
    public static void write(HttpServletResponse response, String text) {
        response.setContentType("application/json;charset=UTF-8");
        try (Writer writer = response.getWriter()) {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new UtilException(e);
        }
    }

    /**
     * 返回数据给客户端
     *
     * @param response    响应对象{@link HttpServletResponse}
     * @param text        返回的内容
     * @param contentType 返回的类型
     */
    public static void write(HttpServletResponse response, String text, String contentType) {
        response.setContentType(contentType);
        try (Writer writer = response.getWriter()) {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new UtilException(e);
        }
    }

}
