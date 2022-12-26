package cn.projectan.strix.utils;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * okhttp3 工具类
 *
 * @author 安炯奕
 * @date 2019/3/26 17:21
 */
@Slf4j
public class OkHttpUtil {
    private static volatile OkHttpClient singleton;

    private OkHttpUtil() {
    }

    public static OkHttpClient getInstance() {
        if (singleton == null) {
            synchronized (OkHttpUtil.class) {
                if (singleton == null) {
                    singleton = new OkHttpClient().newBuilder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(45, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return singleton;
    }

    /**
     * 发起get请求
     */
    public static String httpGet(String url) {
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = getInstance().newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 发起带header，带params的get请求
     */
    public static String httpGet(String url, Map<String, String> headers, Map<String, String> params) {
        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        // 组装请求头
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                requestBuilder.addHeader(key, headers.get(key));
            }
        }
        // 组装请求体
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.url(httpBuilder.build())
                .get().build();
        try {
            Response response = getInstance().newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 发送post请求
     */
    public static String httpPost(String url) {
        String result = null;

        FormBody.Builder builder = new FormBody.Builder();
        FormBody formBody = builder.build();

        Request request = new Request.Builder().url(url).post(formBody).build();
        try {
            Response response = getInstance().newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 发送带参数post请求
     */
    public static String httpPost(String url, Map<String, String> map) {
        String result = null;

        FormBody.Builder builder = new FormBody.Builder();
        for (String key : map.keySet()) {
            builder.add(key, map.get(key));
        }
        FormBody formBody = builder.build();

        Request request = new Request.Builder().url(url).post(formBody).build();
        try {
            Response response = getInstance().newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 发送带header，带params的post请求
     */
    public static String httpPost(String url, Map<String, String> headers, Map<String, String> params) {
        String result = null;

        // 组装请求头
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (MapUtil.isNotEmpty(headers)) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        // 组装请求体
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (MapUtil.isNotEmpty(params)) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                formBuilder.add(param.getKey(), param.getValue());
            }
        }
        FormBody formBody = formBuilder.build();
        requestBuilder.post(formBody);

        Request request = requestBuilder.build();
        try {
            Response response = getInstance().newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 发送请求体为json的post请求
     */
    public static String httpPostWithJson(String url, String json) {
        String result = null;

        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder().url(url).post(requestBody).build();
        // 这里使用了try-with-resources，暂不清楚是否会出现问题
        // 根据OKHttp说明，string()方法会自动关闭response
        try (Response response = getInstance().newCall(request).execute()) {
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 发送请求体为json的post请求
     */
    public static String httpPostWithJson(String url, Map<String, String> headers, String json) {
        String result = null;

        // 组装请求头
        Request.Builder requestBuilder = new Request.Builder().url(url);
        requestBuilder.addHeader("Content-Type", "application/json");
        if (MapUtil.isNotEmpty(headers)) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));

        requestBuilder.method("POST", requestBody);
        Request request = requestBuilder.build();

        try {
            Response response = getInstance().newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String, String> paramSplit(String urlParam) {
        Map<String, String> map = new HashMap<>();
        String[] param = urlParam.split("&");
        for (String keyValue : param) {
            String[] pair = keyValue.split("=");
            if (pair.length == 2) {
                map.put(pair[0], pair[1]);
            }
        }
        return map;
    }

}
