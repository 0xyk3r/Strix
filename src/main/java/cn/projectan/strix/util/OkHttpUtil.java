package cn.projectan.strix.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OkHttp 工具类
 *
 * @author ProjectAn
 * @since 2019/3/26 17:21
 */
@Slf4j
public class OkHttpUtil {

    private static volatile OkHttpClient singleton;

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpUtil() {
    }

    public static OkHttpClient getInstance() {
        if (singleton == null) {
            synchronized (OkHttpUtil.class) {
                if (singleton == null) {
                    singleton = new OkHttpClient().newBuilder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(45, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return singleton;
    }

    /**
     * 发送 GET 请求（HTTP），不附带参数
     *
     * @param url 请求url
     * @return 响应结果
     */
    public static String get(String url) {
        return get(url, null, null);
    }

    /**
     * 发送 GET 请求（HTTP），附带请求参数
     *
     * @param url    请求url
     * @param params 请求参数
     * @return 响应结果
     */
    public static String get(String url, Map<String, String> params) {
        return get(url, null, params);
    }

    /**
     * 发送 GET 请求（HTTP），附带请求头、请求参数
     *
     * @param url     请求url
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     */
    public static String get(String url, Map<String, String> headers, Map<String, String> params) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        // 组装请求头
        Request.Builder requestBuilder = new Request.Builder();
        Optional.ofNullable(headers).ifPresent(header -> header.forEach(requestBuilder::header));
        // 组装请求参数
        Optional.ofNullable(params).ifPresent(param -> param.forEach(httpBuilder::addQueryParameter));
        Request request = requestBuilder
                .url(httpBuilder.build().url())
                .build();
        return execute(request);
    }

    /**
     * 发送 POST 请求（HTTP），不附带参数
     *
     * @param url 请求url
     * @return 响应结果
     */
    public static String post(String url) {
        return post(url, null, null);
    }

    /**
     * 发送 POST 请求（HTTP），附带请求参数（Form）
     *
     * @param url    请求url
     * @param params 请求参数
     * @return 响应结果
     */
    public static String post(String url, Map<String, String> params) {
        return post(url, null, params);
    }

    /**
     * 发送 POST 请求（HTTP），附带请求头、请求参数（Form）
     *
     * @param url     请求url
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     */
    public static String post(String url, Map<String, String> headers, Map<String, String> params) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        // 组装请求头
        Optional.ofNullable(headers).ifPresent(header -> header.forEach(requestBuilder::header));
        // 组装请求参数
        FormBody.Builder formBuilder = new FormBody.Builder();
        Optional.ofNullable(params).ifPresent(param -> param.forEach(formBuilder::add));
        Request request = requestBuilder
                .post(formBuilder.build())
                .build();
        return execute(request);
    }

    /**
     * 发送请求体为json的post请求
     */
    public static String postJson(String url, String json) {
        return postJson(url, null, json);
    }


    /**
     * 发送请求体为json的post请求
     */
    public static String postJson(String url, Map<String, String> headers, String json) {
        Assert.hasText(json, "JSON can not be empty!");
        // 组装请求头
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json");
        Optional.ofNullable(headers).ifPresent(header -> header.forEach(requestBuilder::header));
        // 组装请求参数
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = requestBuilder
                .post(requestBody)
                .build();
        return execute(request);
    }

    public static Map<String, String> parseQueryParamToMap(String queryParam) {
        if (!StringUtils.hasText(queryParam)) {
            return Collections.emptyMap();
        }
        return Arrays.stream(queryParam.split("&"))
                .map(param -> param.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8),
                        (v1, v2) -> v1, // 如果有重复的参数名，保留第一个值
                        HashMap::new));
    }

    private static String execute(Request request) {
        try (Response response = getInstance().newCall(request).execute()) {
            ResponseBody body = response.body();
            return body == null ? null : body.string();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
