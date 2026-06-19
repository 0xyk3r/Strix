package cn.projectan.strix.core.module.ai.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * DashScope 原生 HTTP 客户端
 * <p>基于 OkHttp，无需 DashScope SDK，直接调用 DashScope REST API</p>
 *
 * <ul>
 *   <li>TTS：{@code POST /services/audio/tts/SpeechSynthesizer}（阻塞，需提前注册音色）</li>
 *   <li>音色注册：{@code POST /services/audio/tts/customization}（create_voice / query_voice）</li>
 *   <li>图片生成：{@code POST /services/aigc/multimodal-generation/generation}（同步，qwen-image-2.0-pro）</li>
 *   <li>批量 ASR：{@code POST /services/audio/asr/transcription}（异步任务）</li>
 *   <li>任务轮询：{@code GET /tasks/{taskId}}</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-05-21
 */
@Slf4j
@Component
public class DashScopeHttpClient {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    public DashScopeHttpClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ============================================================
    //  TTS
    // ============================================================

    /**
     * TTS 语音合成（阻塞模式），返回 DashScope 预签名音频 URL
     *
     * <p>使用前需通过 {@link #enrollVoice} 注册音色，并将返回的 voice_id 存入数据库。</p>
     *
     * @param apiKey     API 密钥
     * @param baseUrl    DashScope 原生 API 基础 URL，如 {@code https://dashscope.aliyuncs.com/api/v1}
     * @param model      模型名称，如 {@code cosyvoice-v3.5-plus}
     * @param text       要合成的文本
     * @param voice      音色 ID（通过 enrollVoice 注册后的 voice_id）
     * @param format     音频格式，如 {@code wav}、{@code mp3}
     * @param speed      语速（1.0 = 正常，0.5 = 减速，2.0 = 加速）
     * @param sampleRate 采样率（如 22050）
     * @return 音频文件的预签名 URL
     */
    public String synthesizeSpeechToUrl(String apiKey, String baseUrl, String model,
                                        String text, String voice, String format,
                                        double speed, int sampleRate) {
        String reqBody = JSONUtil.createObj()
                .set("model", model)
                .set("input", JSONUtil.createObj()
                        .set("text", text)
                        .set("voice", voice)
                        .set("format", format != null && !format.isBlank() ? format : "wav")
                        .set("sample_rate", sampleRate > 0 ? sampleRate : 22050)
                        .set("rate", speed > 0 ? speed : 1.0))
                .toJSONString(0);

        String url = normalizeBaseUrl(baseUrl) + "/services/audio/tts/SpeechSynthesizer";
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(reqBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DashScope TTS 请求失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            String audioUrl = json.getByPath("output.audio.url", String.class);
            if (audioUrl == null || audioUrl.isBlank()) {
                throw new RuntimeException("DashScope TTS 响应中未找到 output.audio.url: " + body);
            }
            return audioUrl;
        } catch (IOException e) {
            throw new RuntimeException("DashScope TTS HTTP 请求异常", e);
        }
    }

    // ============================================================
    //  TTS 音色注册（声音复刻）
    // ============================================================

    /**
     * 注册 TTS 音色（声音复刻），提交后立即返回 voice_id
     *
     * <p>音色创建后处于 DEPLOYING 状态，需调用 {@link #pollVoiceUntilReady} 等待审核通过。</p>
     *
     * @param apiKey      API 密钥
     * @param baseUrl     DashScope 原生 API 基础 URL
     * @param targetModel 语音合成模型，如 {@code cosyvoice-v3.5-plus}
     * @param audioUrl    用于复刻音色的参考音频公网 URL
     * @param prefix      音色前缀（仅允许小写字母和数字，不超过 10 个字符）
     * @return voice_id，可直接用于语音合成接口的 voice 参数
     */
    public String enrollVoice(String apiKey, String baseUrl, String targetModel,
                              String audioUrl, String prefix) {
        String url = normalizeBaseUrl(baseUrl) + "/services/audio/tts/customization";
        String reqBody = JSONUtil.createObj()
                .set("model", "voice-enrollment")
                .set("input", JSONUtil.createObj()
                        .set("action", "create_voice")
                        .set("target_model", targetModel)
                        .set("url", audioUrl)
                        .set("prefix", prefix)
                        .set("language_hints", List.of("zh")))
                .toJSONString(0);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(reqBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DashScope 音色注册失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            String voiceId = json.getByPath("output.voice_id", String.class);
            if (voiceId == null || voiceId.isBlank()) {
                throw new RuntimeException("DashScope 音色注册响应中未找到 voice_id: " + body);
            }
            return voiceId;
        } catch (IOException e) {
            throw new RuntimeException("DashScope 音色注册 HTTP 请求异常", e);
        }
    }

    /**
     * 查询音色状态
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL
     * @param voiceId 音色 ID
     * @return 音色状态：{@code DEPLOYING}（审核中）/ {@code OK}（可用）/ {@code UNDEPLOYED}（审核不通过）
     */
    public String queryVoiceStatus(String apiKey, String baseUrl, String voiceId) {
        String url = normalizeBaseUrl(baseUrl) + "/services/audio/tts/customization";
        String reqBody = JSONUtil.createObj()
                .set("model", "voice-enrollment")
                .set("input", JSONUtil.createObj()
                        .set("action", "query_voice")
                        .set("voice_id", voiceId))
                .toJSONString(0);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(reqBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DashScope 音色查询失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            String status = json.getByPath("output.status", String.class);
            if (status == null) {
                throw new RuntimeException("DashScope 音色查询响应中未找到 status: " + body);
            }
            return status;
        } catch (IOException e) {
            throw new RuntimeException("DashScope 音色查询 HTTP 请求异常", e);
        }
    }

    /**
     * 轮询音色审核状态，直到 {@code OK}（可用）或 {@code UNDEPLOYED}（审核不通过），最多等待约 5 分钟
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL
     * @param voiceId 音色 ID
     * @throws RuntimeException 审核不通过或超时
     */
    public void pollVoiceUntilReady(String apiKey, String baseUrl, String voiceId) {
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(i == 0 ? 2_000L : 5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("音色轮询被中断 (voiceId=" + voiceId + ")");
            }

            String status = queryVoiceStatus(apiKey, baseUrl, voiceId);
            log.debug("音色 {} 状态: {}", voiceId, status);

            if ("OK".equals(status)) {
                log.info("音色注册审核通过，可用: voiceId={}", voiceId);
                return;
            }
            if ("UNDEPLOYED".equals(status)) {
                throw new RuntimeException("音色审核不通过 (voiceId=" + voiceId + ")");
            }
            // DEPLOYING — 继续等待
        }
        throw new RuntimeException("音色注册超时（> 5 分钟），voiceId=" + voiceId);
    }

    // ============================================================
    //  同步多模态生成（multimodal-generation：图片生成 / Qwen-Flash 语音识别等共用）
    // ============================================================

    /**
     * 同步调用 multimodal-generation 接口（图片生成 qwen-image / Qwen-Flash 语音识别等共用）。
     *
     * @param apiKey      API 密钥
     * @param baseUrl     DashScope 原生 API 基础 URL
     * @param requestBody JSON 请求体字符串
     * @return output 对象（含 choices 数组）
     */
    public JSONObject multimodalGenerationSync(String apiKey, String baseUrl, String requestBody) {
        String url = normalizeBaseUrl(baseUrl) + "/services/aigc/multimodal-generation/generation";
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DashScope multimodal-generation 请求失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            // 检查 API 级别错误（200 但包含 code 字段）
            if (json.containsKey("code") && !json.containsKey("output")) {
                throw new RuntimeException("DashScope multimodal-generation API 错误: " + body);
            }
            JSONObject output = json.getJSONObject("output");
            if (output == null) {
                throw new RuntimeException("DashScope multimodal-generation 响应中未找到 output 字段: " + body);
            }
            return output;
        } catch (IOException e) {
            throw new RuntimeException("DashScope multimodal-generation HTTP 请求异常", e);
        }
    }

    // ============================================================
    //  异步任务（ASR）
    // ============================================================

    /**
     * 提交异步任务（批量 ASR 或图片生成），返回 task_id
     *
     * @param apiKey      API 密钥
     * @param baseUrl     DashScope 原生 API 基础 URL
     * @param endpoint    相对路径，如 {@code services/audio/asr/transcription}
     * @param requestBody JSON 请求体字符串
     * @return 任务 ID
     */
    public String submitAsyncTask(String apiKey, String baseUrl, String endpoint, String requestBody) {
        String url = normalizeBaseUrl(baseUrl) + "/" + endpoint;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .post(RequestBody.create(requestBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (resp.code() != 200 && resp.code() != 202) {
                throw new RuntimeException("DashScope 异步任务提交失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            String taskId = json.getByPath("output.task_id", String.class);
            if (taskId == null || taskId.isBlank()) {
                throw new RuntimeException("DashScope 响应中未包含 task_id: " + body);
            }
            return taskId;
        } catch (IOException e) {
            throw new RuntimeException("DashScope 异步任务提交 HTTP 异常", e);
        }
    }

    /**
     * 轮询任务结果，直到状态变为 SUCCEEDED 或 FAILED（最多等待约 10 分钟）
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL
     * @param taskId  任务 ID
     * @return 任务完成时的 {@code output} JSON 对象
     */
    public JSONObject pollTaskUntilDone(String apiKey, String baseUrl, String taskId) {
        String url = normalizeBaseUrl(baseUrl) + "/tasks/" + taskId;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        for (int i = 0; i < 120; i++) {
            try {
                Thread.sleep(i == 0 ? 2000L : 5000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("DashScope 任务轮询被中断 (taskId=" + taskId + ")");
            }

            try (Response resp = httpClient.newCall(request).execute()) {
                String body = Objects.requireNonNull(resp.body()).string();
                if (!resp.isSuccessful()) {
                    throw new RuntimeException("DashScope 任务查询失败 [" + resp.code() + "]: " + body);
                }
                JSONObject json = JSONUtil.parseObj(body);
                JSONObject output = json.getJSONObject("output");
                String status = output != null ? output.getStr("task_status") : null;

                if ("SUCCEEDED".equals(status)) {
                    log.info("DashScope 任务完成: taskId={}", taskId);
                    return output;
                }
                if ("FAILED".equals(status)) {
                    throw new RuntimeException("DashScope 任务失败 (taskId=" + taskId + "): " + body);
                }
                log.debug("DashScope 任务 {} 状态: {}, 继续等待...", taskId, status);
            } catch (IOException e) {
                throw new RuntimeException("DashScope 任务查询 HTTP 异常", e);
            }
        }
        throw new RuntimeException("DashScope 任务超时（> 10 分钟），taskId=" + taskId);
    }

    // ============================================================
    //  工具方法
    // ============================================================

    /**
     * 下载 URL 中的字节内容（音频文件、转录 JSON 等）
     *
     * @param url 要下载的 URL
     * @return 字节数组
     */
    public byte[] downloadBytes(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("资源下载失败 [" + resp.code() + "]: " + url);
            }
            return Objects.requireNonNull(resp.body()).bytes();
        } catch (IOException e) {
            throw new RuntimeException("资源下载 HTTP 异常: " + url, e);
        }
    }

    /**
     * 获取内部 OkHttpClient（供 WebSocket 连接使用）
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }
}
