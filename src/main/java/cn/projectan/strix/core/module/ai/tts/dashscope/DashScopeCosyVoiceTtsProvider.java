package cn.projectan.strix.core.module.ai.tts.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.tts.TtsAudioListener;
import cn.projectan.strix.core.module.ai.tts.TtsParams;
import cn.projectan.strix.core.module.ai.tts.TtsProvider;
import cn.projectan.strix.core.module.ai.tts.TtsStreamSession;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DashScope CosyVoice 语音合成 Provider。
 * <p>
 * 同一模型名（cosyvoice-v3.5-plus 等）同时支持三种合成方式：
 * <ul>
 *   <li>非流式 HTTP：{@code /services/audio/tts/SpeechSynthesizer}（返回音频 URL → 下载字节）</li>
 *   <li>HTTP 流式 SSE：同端点 + {@code X-DashScope-SSE: enable}（逐段 Base64 音频）</li>
 *   <li>WebSocket 双向流式：{@code wss://.../api-ws/v1/inference}（run-task/continue-task/finish-task）</li>
 * </ul>
 * WebSocket 会话复用 run-task 协议双向桥接范式：会话既是上行 {@link TtsStreamSession}，
 * 也是下行 okhttp {@link WebSocketListener}；task-started 前缓存待发文本。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeCosyVoiceTtsProvider implements TtsProvider {

    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

    private final DashScopeHttpClient dashScopeHttpClient;

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName();
        return model != null && model.toLowerCase().contains("cosyvoice");
    }

    @Override
    public byte[] synthesize(AiModelConfig config, String text, TtsParams params) {
        String audioUrl = dashScopeHttpClient.synthesizeSpeechToUrl(
                config.getApiKey(), config.getBaseUrl(), config.getModelName(), text, params);
        return dashScopeHttpClient.downloadBytes(audioUrl);
    }

    @Override
    public void synthesizeStream(AiModelConfig config, String text, TtsParams params, TtsAudioListener listener) {
        try {
            dashScopeHttpClient.synthesizeSpeechStream(
                    config.getApiKey(), config.getBaseUrl(), config.getModelName(),
                    text, params, listener::onAudio);
            listener.onCompleted();
        } catch (Exception e) {
            log.error("CosyVoice HTTP 流式合成失败", e);
            listener.onError(e.getMessage());
        }
    }

    @Override
    public TtsStreamSession openStream(AiModelConfig config, TtsParams params, TtsAudioListener listener) {
        String base = config.getBaseUrl();
        String url = (base != null && (base.startsWith("ws://") || base.startsWith("wss://"))) ? base : WS_URL;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();

        JSONObject parameters = buildWsParameters(config.getModelName(), params);
        WsTtsSession session = new WsTtsSession(config.getModelName(), parameters, listener);
        WebSocket ws = dashScopeHttpClient.getHttpClient().newWebSocket(request, session);
        session.attach(ws);
        return session;
    }

    /**
     * 构建 run-task 的 parameters（文本类型固定 PlainText，仅写入非 null 字段）。
     */
    private JSONObject buildWsParameters(String model, TtsParams params) {
        TtsParams p = params != null ? params : TtsParams.empty();
        JSONObject parameters = JSONUtil.createObj()
                .set("text_type", "PlainText")
                .set("voice", p.voice())
                .set("format", p.format() != null && !p.format().isBlank() ? p.format() : "mp3")
                .set("sample_rate", p.sampleRate() != null ? p.sampleRate() : 22050)
                .set("enable_ssml", Boolean.TRUE.equals(p.enableSsml()));
        if (p.volume() != null) {
            parameters.set("volume", p.volume());
        }
        if (p.rate() != null) {
            parameters.set("rate", p.rate());
        }
        if (p.pitch() != null) {
            parameters.set("pitch", p.pitch());
        }
        if (p.bitRate() != null) {
            parameters.set("bit_rate", p.bitRate());
        }
        if (p.instruction() != null && !p.instruction().isBlank()) {
            parameters.set("instruction", p.instruction());
        }
        if (p.seed() != null) {
            parameters.set("seed", p.seed());
        }
        return parameters;
    }

    /**
     * WebSocket 双向流式合成会话：既是上行 {@link TtsStreamSession}，也是下行 {@link WebSocketListener}。
     * <p>建连后发送 run-task；task-started 前的 sendText 缓存，task-started 后冲刷为 continue-task；
     * finish 发送 finish-task；二进制音频帧经 listener 实时下发。
     */
    private static class WsTtsSession extends WebSocketListener implements TtsStreamSession {

        private final String model;
        private final JSONObject parameters;
        private final String taskId;
        private final TtsAudioListener listener;
        private volatile WebSocket ws;
        private volatile boolean taskReady = false;
        private volatile boolean finishRequested = false;
        private final java.util.concurrent.ConcurrentLinkedQueue<String> pendingText = new java.util.concurrent.ConcurrentLinkedQueue<>();

        WsTtsSession(String model, JSONObject parameters, TtsAudioListener listener) {
            this.model = model;
            this.parameters = parameters;
            this.listener = listener;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        void attach(WebSocket ws) {
            this.ws = ws;
        }

        @Override
        public void sendText(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (!taskReady) {
                pendingText.add(text);
                return;
            }
            sendContinueTask(text);
        }

        @Override
        public void finish() {
            finishRequested = true;
            WebSocket w = ws;
            if (w == null || !taskReady) {
                return;
            }
            sendFinishTask();
        }

        @Override
        public void close() {
            WebSocket w = ws;
            if (w != null) {
                ws = null;
                w.close(1000, "client closed");
            }
        }

        private void sendContinueTask(String text) {
            WebSocket w = ws;
            if (w == null) {
                return;
            }
            String msg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "continue-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj()
                            .set("input", JSONUtil.createObj().set("text", text)))
                    .toJSONString(0);
            w.send(msg);
        }

        private void sendFinishTask() {
            WebSocket w = ws;
            if (w == null) {
                return;
            }
            String msg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "finish-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj().set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            w.send(msg);
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            String runTaskMsg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "run-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj()
                            .set("task_group", "audio")
                            .set("task", "tts")
                            .set("function", "SpeechSynthesizer")
                            .set("model", model)
                            .set("parameters", parameters)
                            .set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            webSocket.send(runTaskMsg);
            log.info("run-task TTS 已发送: taskId={}, model={}", taskId, model);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject msg = JSONUtil.parseObj(text);
                JSONObject header = msg.getJSONObject("header");
                String event = header != null ? header.getStr("event") : null;
                if ("task-started".equals(event)) {
                    taskReady = true;
                    String t;
                    while ((t = pendingText.poll()) != null) {
                        sendContinueTask(t);
                    }
                    if (finishRequested) {
                        sendFinishTask();
                    }
                } else if ("task-finished".equals(event)) {
                    listener.onCompleted();
                } else if ("task-failed".equals(event)) {
                    String errMsg = header != null ? header.getStr("error_message", "语音合成任务失败") : "语音合成任务失败";
                    listener.onError(errMsg);
                }
                // result-generated 仅含句级元信息，音频经二进制帧下发，无需处理
            } catch (Exception e) {
                log.error("解析 run-task TTS 消息出错", e);
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
            byte[] audio = bytes.toByteArray();
            if (audio.length > 0) {
                listener.onAudio(audio);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            log.error("run-task TTS 连接失败: taskId={}", taskId, t);
            listener.onError("语音合成连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("run-task TTS 连接关闭: taskId={}, code={}, reason={}", taskId, code, reason);
        }
    }
}

