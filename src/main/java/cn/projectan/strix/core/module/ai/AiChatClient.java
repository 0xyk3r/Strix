package cn.projectan.strix.core.module.ai;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper MAPPER = AiJson.mapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * OkHttp 客户端（可配置超时）。
     * <p>超时通过 {@code strix.ai.http.*} 配置，未配置时使用默认值。
     * 深度思考 + 长输出模型建议将 {@code read-timeout-seconds} 调大。
     */
    private final OkHttpClient httpClient;

    public AiChatClient(
            @Value("${strix.ai.http.connect-timeout-seconds:30}") long connectTimeoutSeconds,
            @Value("${strix.ai.http.read-timeout-seconds:300}") long readTimeoutSeconds,
            @Value("${strix.ai.http.write-timeout-seconds:30}") long writeTimeoutSeconds) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .writeTimeout(Duration.ofSeconds(writeTimeoutSeconds))
                .build();
    }

    /**
     * 非流式聊天（同步阻塞，返回完整响应 JsonNode）
     *
     * @param baseUrl API 基础 URL（如 <a href="https://dashscope.aliyuncs.com/compatible-mode/v1">...</a>）
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

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
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
        streamChat(baseUrl, apiKey, body, handler, null);
    }

    /**
     * 流式聊天（SSE），带上游连接取消回调。
     * <p>
     * {@code callConsumer} 在发起请求前收到底层 {@link Call} 引用，调用方可持有它并在需要时
     * （如用户主动停止生成）从其它线程调用 {@link Call#cancel()} 立即中断阻塞的流读取。
     * 这解决了「深度思考模型长时间不产出新 chunk 时，仅靠 handler 抛异常无法及时停止」的问题：
     * cancel 会让阻塞中的 {@code readLine()} 立刻抛出 IOException，从而及时收尾落库。
     *
     * @param callConsumer 在 execute 前接收 Call 引用的回调（可为 null）
     */
    public void streamChat(String baseUrl, String apiKey, Map<String, Object> body,
                           SseChunkHandler handler, java.util.function.Consumer<Call> callConsumer) throws IOException {
        body.put("stream", true);
        String url = normalizeUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        Call call = httpClient.newCall(request);
        if (callConsumer != null) {
            callConsumer.accept(call);
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("AI API 返回错误 " + response.code() + ": " + errorBody);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
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

    /**
     * FIM (Fill-In-Middle) 补全（Beta 端点，同步阻塞）
     * <p>
     * 使用 {@code /completions}（而非 /chat/completions），请求体包含 {@code prompt} 和可选
     * {@code suffix}。最大 Token 数受限 4K（DeepSeek Beta FIM 限制）。
     *
     * @param baseUrl API 基础 URL（如 <a href="https://api.deepseek.com/beta">...</a>）
     * @param apiKey  API Key（Bearer token）
     * @param body    请求体 Map（包含 model, prompt, suffix?, max_tokens, temperature? 等）
     * @return 完整响应 JsonNode
     * @throws IOException 网络/HTTP 错误
     */
    public JsonNode fim(String baseUrl, String apiKey, Map<String, Object> body) throws IOException {
        String url = normalizeFimUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("AI FIM API 返回错误 " + response.code() + ": " + responseBody);
            }
            return MAPPER.readTree(responseBody);
        }
    }

    /**
     * FIM (Fill-In-Middle) 流式补全（Beta 端点，SSE）
     * <p>
     * 同 {@link #fim} 但设置 {@code stream=true}，逐 chunk 回调。
     * chunk 格式：{@code {"choices":[{"text":"...","finish_reason":null}]}}。
     *
     * @param baseUrl API 基础 URL
     * @param apiKey  API Key
     * @param body    请求体（不含 stream 字段，由此方法自动追加）
     * @param handler 每个非 [DONE] data chunk 的回调
     * @throws IOException 网络/HTTP/流读取错误
     */
    public void streamFim(String baseUrl, String apiKey, Map<String, Object> body,
                          SseChunkHandler handler) throws IOException {
        body.put("stream", true);
        String url = normalizeFimUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("AI FIM API 返回错误 " + response.code() + ": " + errorBody);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        handler.onChunk(MAPPER.readTree(data));
                    } catch (Exception e) {
                        log.debug("AI FIM: 解析 SSE chunk 失败，跳过: {}", data);
                    }
                }
            }
        }
    }

    private String normalizeFimUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "") + "/completions";
    }
}
