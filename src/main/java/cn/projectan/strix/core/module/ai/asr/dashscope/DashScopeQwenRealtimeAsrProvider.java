package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.*;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 阿里云百炼 qwen-asr-realtime 系列（如 {@code qwen3-asr-flash-realtime}）实时识别 Provider。
 * <p>
 * 采用 OpenAI-Realtime 风格协议：
 * <ul>
 *   <li>连接 {@code wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=<model>}，
 *       Header 携带 {@code Authorization: Bearer <key>} 与 {@code OpenAI-Beta: realtime=v1}</li>
 *   <li>连接建立后发送 {@code session.update}（pcm/16k，server_vad 自动断句）</li>
 *   <li>音频以 {@code input_audio_buffer.append}（Base64）持续上行</li>
 *   <li>中间结果通过 {@code conversation.item.input_audio_transcription.text}（text+stash）返回，
 *       最终结果通过 {@code conversation.item.input_audio_transcription.completed}（transcript）返回</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
@Component
@RequiredArgsConstructor
public class DashScopeQwenRealtimeAsrProvider implements RealtimeAsrProvider {

    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
    private static final AtomicLong EVENT_SEQ = new AtomicLong();

    private final DashScopeHttpClient dashScopeHttpClient;

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        // qwen 系列实时 ASR（qwen3-asr-flash-realtime 等）
        return model.contains("qwen") && model.contains("asr");
    }

    @Override
    public RealtimeAsrSession open(AiModelConfig config, AsrSessionParams params, AsrResultListener listener) {
        String url = resolveWsUrl(config.getBaseUrl()) + "?model=" + config.getModelName();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("OpenAI-Beta", "realtime=v1")
                .build();

        QwenRealtimeSession session = new QwenRealtimeSession(config, listener);
        WebSocket ws = dashScopeHttpClient.getHttpClient().newWebSocket(request, session);
        session.attach(ws);
        return session;
    }

    /**
     * 优先使用配置中的 ws(s) 端点，否则回退到默认 DashScope realtime 端点
     */
    private static String resolveWsUrl(String baseUrl) {
        String base = (baseUrl != null && (baseUrl.startsWith("ws://") || baseUrl.startsWith("wss://")))
                ? baseUrl : WS_URL;
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String nextEventId() {
        return "event_" + EVENT_SEQ.incrementAndGet();
    }

    /**
     * qwen-asr-realtime 会话：既是 RealtimeAsrSession（上行）也是 okhttp WebSocketListener（下行）。
     */
    @Slf4j
    private static class QwenRealtimeSession extends WebSocketListener implements RealtimeAsrSession {

        private final AiModelConfig config;
        private final AsrResultListener listener;
        private volatile WebSocket ws;
        /**
         * session.update 是否已被服务端确认（收到 session.updated）；之前到达的音频需缓存，保证 session.update 为首条消息
         */
        private volatile boolean sessionReady = false;
        private final java.util.concurrent.ConcurrentLinkedQueue<byte[]> pendingAudio = new java.util.concurrent.ConcurrentLinkedQueue<>();
        /**
         * 就绪前最多缓存的音频帧数（约 100ms/帧），防止异常情况下无限增长
         */
        private static final int MAX_PENDING_FRAMES = 200;

        QwenRealtimeSession(AiModelConfig config, AsrResultListener listener) {
            this.config = config;
            this.listener = listener;
        }

        void attach(WebSocket ws) {
            this.ws = ws;
        }

        // ——— 上行 RealtimeAsrSession ———

        @Override
        public void sendAudio(byte[] pcm) {
            WebSocket w = ws;
            if (w == null || pcm == null || pcm.length == 0) return;
            // session.update 未被确认前先缓存，避免音频先于 session.update 到达服务端（否则报 session already started）
            if (!sessionReady) {
                log.warn("qwen-asr-realtime 音频到达时会话未就绪，先行缓存: pendingAudio.size={}", pendingAudio.size());
                if (pendingAudio.size() < MAX_PENDING_FRAMES) {
                    pendingAudio.add(pcm);
                }
                return;
            }
            sendAppend(w, pcm);
        }

        private void sendAppend(WebSocket w, byte[] pcm) {
            String event = JSONUtil.createObj()
                    .set("event_id", nextEventId())
                    .set("type", "input_audio_buffer.append")
                    .set("audio", Base64.getEncoder().encodeToString(pcm))
                    .toJSONString(0);
            w.send(event);
        }

        @Override
        public void finish() {
            // 通知服务端结束会话：冲刷最后一段语音的识别结果，并回送 session.finished（官方示例在音频结束后发送）
            WebSocket w = ws;
            if (w == null) return;
            String event = JSONUtil.createObj()
                    .set("event_id", nextEventId())
                    .set("type", "session.finish")
                    .toJSONString(0);
            w.send(event);
        }

        @Override
        public void close() {
            WebSocket w = ws;
            if (w != null) {
                ws = null;
                w.close(1000, "client closed");
            }
        }

        // ——— 下行 okhttp WebSocketListener ———

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            String lang = StringUtils.hasText(config.getLanguage()) ? config.getLanguage() : null;
            JSONObject session = JSONUtil.createObj()
                    .set("input_audio_format", "pcm")
                    .set("sample_rate", 16000)
                    .set("input_audio_transcription", JSONUtil.createObj().set("language", lang))
                    .set("turn_detection", JSONUtil.createObj()
                            .set("type", "server_vad")
                            // VAD 灵敏度阈值：取 0.0（最灵敏），将收到的音频均视为语音。
                            // 前端已做 RMS VAD、仅在说话时上行，故断句完全交由 silence_duration_ms 控制。
                            .set("threshold", 0.0)
                            // 断句静音阈值：400ms。句末静音累计超过该值即提交一句最终结果。
                            // 取值偏短可让 final 结果更快返回；过长则整句迟迟不出。
                            .set("silence_duration_ms", 400));
            String event = JSONUtil.createObj()
                    .set("type", "session.update")
                    .set("event_id", nextEventId())
                    .set("session", session)
                    .toJSONString(0);
            webSocket.send(event);
            log.info("qwen-asr-realtime session.update 已发送: model={}", config.getModelName());
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject data = JSONUtil.parseObj(text);
                String type = data.getStr("type", "");

                if ("session.created".equals(type) || "session.updated".equals(type)) {
                    // 收到服务端会话事件即视为就绪：此时 session.update（在 onOpen 发出）已先于音频进入有序流。
                    // session.created 必定下发，作为放行音频的可靠信号，避免依赖 session.updated 是否发送。
                    if (!sessionReady) {
                        sessionReady = true;
                        WebSocket w = ws;
                        if (w != null) {
                            byte[] p;
                            while ((p = pendingAudio.poll()) != null) {
                                sendAppend(w, p);
                            }
                        }
                        log.info("qwen-asr-realtime 会话就绪（{}），开始上行音频", type);
                    }
                } else if ("conversation.item.input_audio_transcription.text".equals(type)) {
                    // 中间结果：text=已确认文本，stash=未确认尾部，拼接为当前轮的实时展示（替换式，非追加）
                    String partial = data.getStr("text", "") + data.getStr("stash", "");
                    if (!partial.isEmpty()) {
                        // 顶层 item_id 标识当前句，emotion/language 为该句的情绪与语种（Qwen-ASR 固定返回）
                        String emo = data.getStr("emotion");
                        listener.onTranscript(new AsrTranscript(
                                data.getStr("item_id"), partial, false,
                                emo, emo != null ? "qwen7" : null, null,
                                data.getStr("language"), null, null, null));
                    }
                } else if ("conversation.item.input_audio_transcription.completed".equals(type)) {
                    // 最终结果：transcript 为该轮完整文本（含标点）
                    String transcript = data.getStr("transcript", "");
                    log.info("qwen-asr-realtime 最终结果: transcript='{}'", transcript); // 诊断用，可下调级别
                    if (!transcript.isEmpty()) {
                        String emoFinal = data.getStr("emotion");
                        listener.onTranscript(new AsrTranscript(
                                data.getStr("item_id"), transcript, true,
                                emoFinal, emoFinal != null ? "qwen7" : null, null,
                                data.getStr("language"), null, null, null));
                    }
                } else if ("session.finished".equals(type)) {
                    // 会话正常结束（通常由客户端 session.finish 触发）
                    listener.onCompleted();
                } else if ("error".equals(type) || data.containsKey("error")) {
                    JSONObject err = data.getJSONObject("error");
                    String msg = err != null ? err.getStr("message", "ASR 识别错误")
                            : data.getStr("message", "ASR 识别错误");
                    log.warn("qwen-asr-realtime 错误事件: {}", text); // 诊断用
                    listener.onError(msg);
                } else {
                    log.info("qwen-asr-realtime 未处理事件: type={}, raw={}", type, text); // 诊断用：捕获 response.* 等事件原文
                }
            } catch (Exception e) {
                log.error("解析 qwen-asr-realtime 消息出错", e);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            log.error("qwen-asr-realtime 连接失败", t);
            listener.onError("实时识别连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("qwen-asr-realtime 连接关闭: code={}, reason={}", code, reason);
            // 1000 为正常关闭；其余（如 1007 协议错误）作为错误上报，便于前端展示真实原因
            if (code == 1000) {
                listener.onCompleted();
            } else {
                listener.onError(StringUtils.hasText(reason) ? reason : ("实时识别连接异常关闭 (code=" + code + ")"));
            }
        }
    }
}
