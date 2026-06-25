package cn.projectan.strix.core.module.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 统一 AI 聊天客户端（OkHttp）
 * <p>
 * 支持流式（SSE）和非流式两种调用模式，所有 /chat/completions 请求均通过此类发出。
 * 此类替代了原有的 OpenAI Java SDK 流式传输和 Spring AI 非流式调用。
 * <p>
 * <b>线程安全：</b>OkHttpClient 是线程安全的，该 Bean 为单例。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
@Component
public class AiChatClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 非流式聊天（同步阻塞，返回完整响应 JsonNode）
     *
     * @param baseUrl API 基础 URL（如 https://dashscope.aliyuncs.com/compatible-mode/v1）
     * @param apiKey  API Key（Bearer token）
     * @param body    请求体 Map（不含 stream 字段，由此方法自动设为 false）
     * @return 完整响应 JsonNode
     * @throws IOException 网络/HTTP 错误
     */
    public JsonNode chat(String baseUrl, String apiKey, Map<String, Object> body) throws IOException {
        body.put("stream", false);
        String url = normalizeUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("AI API 返回错误 " + response.code() + ": " + responseBody);
            }
            return MAPPER.readTree(responseBody);
        }
    }

    /**
     * 流式聊天（SSE，逐行回调 chunk JsonNode）
     *
     * @param baseUrl API 基础 URL
     * @param apiKey  API Key
     * @param body    请求体 Map（不含 stream 字段，由此方法自动设为 true）
     * @param handler 每个 SSE data chunk 的回调（chunk 为解析后的 JsonNode）
     * @throws IOException 网络/HTTP/流读取错误
     */
    public void streamChat(String baseUrl, String apiKey, Map<String, Object> body,
                           SseChunkHandler handler) throws IOException {
        body.put("stream", true);
        String url = normalizeUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("AI API 返回错误 " + response.code() + ": " + errorBody);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        handler.onChunk(MAPPER.readTree(data));
                    } catch (Exception e) {
                        log.debug("AI: 解析 SSE chunk 失败，跳过: {}", data);
                    }
                }
            }
        }
    }

    /**
     * SSE chunk 回调接口（每个有效 data 行触发一次）
     */
    @FunctionalInterface
    public interface SseChunkHandler {
        void onChunk(JsonNode chunk) throws IOException;
    }

    private Request buildRequest(String url, String apiKey, String jsonBody) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();
    }

    private String normalizeUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }
}
