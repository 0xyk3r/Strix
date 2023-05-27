package cn.projectan.strix.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2023/5/23 14:16
 */
public class RequestUtils {

    /**
     * 将 QueryString 转换为 Map
     *
     * @param request HttpServletRequest
     * @return Map
     */
    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (!StringUtils.hasText(queryString)) {
            return Map.of();
        }
        return Arrays.stream(queryString.split("&"))
                .map(param -> param.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8),
                        (v1, v2) -> v1, // 如果有重复的参数名，保留第一个值
                        LinkedHashMap::new));
    }

}
