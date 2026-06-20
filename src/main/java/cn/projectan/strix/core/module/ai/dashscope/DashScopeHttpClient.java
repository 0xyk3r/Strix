package cn.projectan.strix.core.module.ai.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.tts.TtsParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
     * <p>使用前需通过声音复刻/设计创建音色，并将 voice_id 传入 {@code params.voice()}。</p>
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL，如 {@code https://dashscope.aliyuncs.com/api/v1}
     * @param model   模型名称，如 {@code cosyvoice-v3.5-plus}
     * @param text    要合成的文本
     * @param params  合并后的合成参数（voice/format/sampleRate/rate/pitch/volume/instruction/enableSsml 等）
     * @return 音频文件的预签名 URL
     */
    public String synthesizeSpeechToUrl(String apiKey, String baseUrl, String model,
                                        String text, TtsParams params) {
        String reqBody = JSONUtil.createObj()
                .set("model", model)
                .set("input", buildTtsInput(text, params))
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

    /**
     * TTS 流式语音合成（HTTP SSE），逐段回调 Base64 解码后的音频字节。
     *
     * <p>添加 {@code X-DashScope-SSE: enable} 头，服务端以 SSE 返回，data 中含 output.audio.data（Base64 PCM/音频块）。</p>
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL
     * @param model   模型名称
     * @param text    要合成的文本
     * @param params  合并后的合成参数
     * @param onAudio 音频块回调（已 Base64 解码）
     */
    public void synthesizeSpeechStream(String apiKey, String baseUrl, String model,
                                       String text, TtsParams params, Consumer<byte[]> onAudio) {
        String reqBody = JSONUtil.createObj()
                .set("model", model)
                .set("input", buildTtsInput(text, params))
                .toJSONString(0);

        String url = normalizeBaseUrl(baseUrl) + "/services/audio/tts/SpeechSynthesizer";
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-SSE", "enable")
                .post(RequestBody.create(reqBody, JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                String body = resp.body() != null ? resp.body().string() : "";
                throw new RuntimeException("DashScope TTS 流式请求失败 [" + resp.code() + "]: " + body);
            }
            ResponseBody respBody = resp.body();
            if (respBody == null) {
                throw new RuntimeException("DashScope TTS 流式响应体为空");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(respBody.byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    try {
                        JSONObject json = JSONUtil.parseObj(data);
                        String b64 = json.getByPath("output.audio.data", String.class);
                        if (b64 != null && !b64.isBlank()) {
                            onAudio.accept(Base64.getDecoder().decode(b64));
                        }
                    } catch (Exception e) {
                        log.debug("跳过无法解析的 TTS SSE 数据行: {}", data);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("DashScope TTS 流式 HTTP 请求异常", e);
        }
    }

    /**
     * 构建 TTS input 对象（仅写入非 null 字段，voice 必填由调用方保证）。
     */
    private JSONObject buildTtsInput(String text, TtsParams params) {
        TtsParams p = params != null ? params : TtsParams.empty();
        JSONObject input = JSONUtil.createObj()
                .set("text", text)
                .set("voice", p.voice())
                .set("format", p.format() != null && !p.format().isBlank() ? p.format() : "mp3")
                .set("sample_rate", p.sampleRate() != null ? p.sampleRate() : 22050);
        if (p.volume() != null) {
            input.set("volume", p.volume());
        }
        if (p.rate() != null) {
            input.set("rate", p.rate());
        }
        if (p.pitch() != null) {
            input.set("pitch", p.pitch());
        }
        if (p.bitRate() != null) {
            input.set("bit_rate", p.bitRate());
        }
        if (p.instruction() != null && !p.instruction().isBlank()) {
            input.set("instruction", p.instruction());
        }
        if (Boolean.TRUE.equals(p.enableSsml())) {
            input.set("enable_ssml", true);
        }
        if (p.seed() != null) {
            input.set("seed", p.seed());
        }
        if (p.languageHints() != null && !p.languageHints().isEmpty()) {
            input.set("language_hints", p.languageHints());
        }
        return input;
    }

    // ============================================================
    //  TTS 音色管理（声音复刻 / 声音设计）
    // ============================================================

    /**
     * 声音复刻：上传参考音频 URL 创建音色，提交后立即返回 voice_id
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
        JSONObject input = JSONUtil.createObj()
                .set("action", "create_voice")
                .set("target_model", targetModel)
                .set("url", audioUrl)
                .set("prefix", prefix)
                .set("language_hints", List.of("zh"));
        JSONObject output = postVoiceCustomization(apiKey, baseUrl, "voice-enrollment", input, null);
        String voiceId = output.getStr("voice_id");
        if (voiceId == null || voiceId.isBlank()) {
            throw new RuntimeException("DashScope 声音复刻响应中未找到 voice_id: " + output);
        }
        return voiceId;
    }

    /**
     * 声音设计：用文字描述创建音色，返回 voice_id 与预览音频（Base64）
     *
     * @param apiKey         API 密钥
     * @param baseUrl        DashScope 原生 API 基础 URL
     * @param targetModel    语音合成模型，如 {@code cosyvoice-v3.5-plus}
     * @param voicePrompt    声音描述文本（≤500 字符）
     * @param previewText    预览音频朗读文本
     * @param prefix         音色前缀（仅允许小写字母和数字，不超过 10 个字符）
     * @param sampleRate     预览音频采样率
     * @param responseFormat 预览音频格式
     * @return [0]=voice_id，[1]=预览音频 Base64（可能为 null）
     */
    public String[] designVoice(String apiKey, String baseUrl, String targetModel,
                                String voicePrompt, String previewText, String prefix,
                                int sampleRate, String responseFormat) {
        JSONObject input = JSONUtil.createObj()
                .set("action", "create_voice")
                .set("target_model", targetModel)
                .set("voice_prompt", voicePrompt)
                .set("preview_text", previewText)
                .set("prefix", prefix);
        JSONObject parameters = JSONUtil.createObj()
                .set("sample_rate", sampleRate > 0 ? sampleRate : 24000)
                .set("response_format", responseFormat != null && !responseFormat.isBlank() ? responseFormat : "wav");
        JSONObject output = postVoiceCustomization(apiKey, baseUrl, "voice-enrollment", input, parameters);
        String voiceId = output.getStr("voice_id");
        if (voiceId == null || voiceId.isBlank()) {
            throw new RuntimeException("DashScope 声音设计响应中未找到 voice_id: " + output);
        }
        String previewAudio = output.getByPath("preview_audio.data", String.class);
        return new String[]{voiceId, previewAudio};
    }

    /**
     * 查询音色状态
     *
     * @return 音色状态：{@code DEPLOYING}（审核中）/ {@code OK}（可用）/ {@code UNDEPLOYED}（审核不通过）
     */
    public String queryVoiceStatus(String apiKey, String baseUrl, String voiceId) {
        JSONObject input = JSONUtil.createObj()
                .set("action", "query_voice")
                .set("voice_id", voiceId);
        JSONObject output = postVoiceCustomization(apiKey, baseUrl, "voice-enrollment", input, null);
        String status = output.getStr("status");
        if (status == null) {
            throw new RuntimeException("DashScope 音色查询响应中未找到 status: " + output);
        }
        return status;
    }

    /**
     * 删除音色
     */
    public void deleteVoice(String apiKey, String baseUrl, String voiceId) {
        JSONObject input = JSONUtil.createObj()
                .set("action", "delete_voice")
                .set("voice_id", voiceId);
        postVoiceCustomization(apiKey, baseUrl, "voice-enrollment", input, null);
    }

    /**
     * 查询账号下的全部音色（分页拉取并汇总），返回每个音色的原始 JSON
     * （含 {@code voice_id}、{@code status}、{@code gmt_create}、{@code gmt_modified}）。
     *
     * @param apiKey  API 密钥
     * @param baseUrl DashScope 原生 API 基础 URL
     * @param prefix  音色前缀过滤（可空，空则拉全部）
     * @return 音色 JSON 列表
     */
    public List<JSONObject> listVoices(String apiKey, String baseUrl, String prefix) {
        List<JSONObject> all = new java.util.ArrayList<>();
        int pageIndex = 0;
        int pageSize = 100;
        // 最多拉 10 页（1000 个），与账号音色上限一致
        for (int page = 0; page < 10; page++) {
            JSONObject input = JSONUtil.createObj()
                    .set("action", "list_voice")
                    .set("page_index", pageIndex)
                    .set("page_size", pageSize);
            if (prefix != null && !prefix.isBlank()) {
                input.set("prefix", prefix);
            }
            JSONObject output = postVoiceCustomization(apiKey, baseUrl, "voice-enrollment", input, null);
            cn.hutool.json.JSONArray arr = output.getJSONArray("voice_list");
            if (arr == null || arr.isEmpty()) {
                break;
            }
            for (Object o : arr) {
                all.add((JSONObject) o);
            }
            if (arr.size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        return all;
    }

    /**
     * 提交音色定制请求（create_voice / query_voice / delete_voice 等），返回 output 对象。
     */
    private JSONObject postVoiceCustomization(String apiKey, String baseUrl, String model,
                                              JSONObject input, JSONObject parameters) {
        String url = normalizeBaseUrl(baseUrl) + "/services/audio/tts/customization";
        JSONObject reqObj = JSONUtil.createObj()
                .set("model", model)
                .set("input", input);
        if (parameters != null) {
            reqObj.set("parameters", parameters);
        }
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(reqObj.toJSONString(0), JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(resp.body()).string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DashScope 音色定制失败 [" + resp.code() + "]: " + body);
            }
            JSONObject json = JSONUtil.parseObj(body);
            JSONObject output = json.getJSONObject("output");
            if (output == null) {
                throw new RuntimeException("DashScope 音色定制响应中未找到 output: " + body);
            }
            return output;
        } catch (IOException e) {
            throw new RuntimeException("DashScope 音色定制 HTTP 请求异常", e);
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
