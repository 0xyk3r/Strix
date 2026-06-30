package cn.projectan.strix.core.module.ai.livetranslate.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.livetranslate.*;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 阿里云百炼 qwen3.5-livetranslate-flash-realtime 实时语音翻译 Provider。
 * <p>
 * 采用 OpenAI-Realtime 风格协议（与 qwen-asr-realtime 相同端点）：
 * <ul>
 *   <li>连接 {@code wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=qwen3.5-livetranslate-flash-realtime}，
 *       Header 携带 {@code Authorization: Bearer <key>} 与 {@code OpenAI-Beta: realtime=v1}</li>
 *   <li>连接建立后发送 {@code session.update}（配置源/目标语种、音色、输出模态等）</li>
 *   <li>音频以 {@code input_audio_buffer.append}（Base64）持续上行</li>
 *   <li>源语言转写通过 {@code conversation.item.input_audio_transcription.text/completed} 返回（需配置 model）</li>
 *   <li>翻译文本通过 {@code response.audio_transcript.text/done}（有音频时）或
 *       {@code response.text.text/done}（纯文本时）返回</li>
 *   <li>翻译音频通过 {@code response.audio.delta/done} 返回（Base64 PCM）</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-30
 */
@Component
@RequiredArgsConstructor
public class DashScopeQwenLiveTranslateProvider implements RealtimeLiveTranslateProvider {

    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
    private static final AtomicLong EVENT_SEQ = new AtomicLong();

    private final DashScopeHttpClient dashScopeHttpClient;

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("livetranslate") || model.contains("live-translate") || model.contains("live_translate");
    }

    @Override
    public RealtimeLiveTranslateSession open(AiModelConfig config, LiveTranslateSessionParams params,
                                             LiveTranslateResultListener listener) {
        String url = resolveWsUrl(config.getBaseUrl()) + "?model=" + config.getModelName();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("OpenAI-Beta", "realtime=v1")
                .build();

        QwenLiveTranslateSession session = new QwenLiveTranslateSession(params, listener);
        WebSocket ws = dashScopeHttpClient.getHttpClient().newWebSocket(request, session);
        session.attach(ws);
        return session;
    }

    private static String resolveWsUrl(String baseUrl) {
        String base = (baseUrl != null && (baseUrl.startsWith("ws://") || baseUrl.startsWith("wss://")))
                ? baseUrl : WS_URL;
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String nextEventId() {
        return "event_lt_" + EVENT_SEQ.incrementAndGet();
    }

    /**
     * qwen-livetranslate 会话：既是 RealtimeLiveTranslateSession（上行）也是 okhttp WebSocketListener（下行）。
     */
    @Slf4j
    private static class QwenLiveTranslateSession extends WebSocketListener implements RealtimeLiveTranslateSession {

        private final LiveTranslateSessionParams params;
        private final LiveTranslateResultListener listener;
        private volatile WebSocket ws;

        /**
         * session.update 是否已被服务端确认；之前到达的音频先行缓存
         */
        private volatile boolean sessionReady = false;
        private final java.util.concurrent.ConcurrentLinkedQueue<byte[]> pendingAudio = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private static final int MAX_PENDING_FRAMES = 200;
        /**
         * 已下发过中间结果的源语言识别句子 itemId 集合。
         * 用于 completed 返回空 transcript 时判断是否需通知前端撤回（同 ASR 的 shownItemIds 机制）。
         * 仅下行回调线程访问，用普通 HashSet 即可。
         */
        private final java.util.Set<String> shownSourceItemIds = new java.util.HashSet<>();

        QwenLiveTranslateSession(LiveTranslateSessionParams params, LiveTranslateResultListener listener) {
            this.params = params;
            this.listener = listener;
        }

        void attach(WebSocket ws) {
            this.ws = ws;
        }
        // ——— 上行 ———

        @Override
        public void sendAudio(byte[] pcm) {
            WebSocket w = ws;
            if (w == null || pcm == null || pcm.length == 0) return;
            if (!sessionReady) {
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

        // ——— 下行 ———

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            // 构建 session.update
            JSONObject sessionObj = JSONUtil.createObj()
                    .set("input_audio_format", "pcm")
                    .set("sample_rate", params.effectiveSampleRate())
                    .set("output_audio_format", "pcm")
                    .set("modalities", params.effectiveModalities())
                    .set("voice", params.effectiveVoice());

            // 源语言转写（可选，配置后会同时返回源语言原文）
            if (params.effectiveEnableSourceTranscription()) {
                JSONObject transcription = JSONUtil.createObj()
                        .set("model", "qwen3-asr-flash-realtime");
                if (StringUtils.hasText(params.sourceLanguage())) {
                    transcription.set("language", params.sourceLanguage());
                }
                sessionObj.set("input_audio_transcription", transcription);
            } else if (StringUtils.hasText(params.sourceLanguage())) {
                sessionObj.set("input_audio_transcription",
                        JSONUtil.createObj().set("language", params.sourceLanguage()));
            }

            // 翻译配置
            JSONObject translation = JSONUtil.createObj()
                    .set("language", params.effectiveTargetLanguage());
            if (params.hotwords() != null && !params.hotwords().isEmpty()) {
                JSONObject phrases = JSONUtil.createObj();
                for (Map.Entry<String, String> e : params.hotwords().entrySet()) {
                    phrases.set(e.getKey(), e.getValue());
                }
                translation.set("corpus", JSONUtil.createObj().set("phrases", phrases));
            }
            sessionObj.set("translation", translation);

            // 声音复刻（可选）
            if (Boolean.TRUE.equals(params.enableVoiceClone())) {
                sessionObj.set("enable_voice_clone", true);
                if (StringUtils.hasText(params.voiceCloneFrequency())) {
                    sessionObj.set("voice_clone_options",
                            JSONUtil.createObj().set("frequency", params.voiceCloneFrequency()));
                }
                sessionObj.set("voice", "default");
            }

            String event = JSONUtil.createObj()
                    .set("event_id", nextEventId())
                    .set("type", "session.update")
                    .set("session", sessionObj)
                    .toJSONString(0);
            webSocket.send(event);
            log.info("livetranslate session.update 已发送: targetLang={}, voice={}, modalities={}",
                    params.effectiveTargetLanguage(), params.effectiveVoice(), params.effectiveModalities());
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject data = JSONUtil.parseObj(text);
                String type = data.getStr("type", "");

                switch (type) {
                    case "session.created", "session.updated" -> {
                        if (!sessionReady) {
                            sessionReady = true;
                            WebSocket w = ws;
                            if (w != null) {
                                byte[] p;
                                while ((p = pendingAudio.poll()) != null) {
                                    sendAppend(w, p);
                                }
                            }
                            log.info("livetranslate 会话就绪（{}），开始上行音频", type);
                        }
                    }
                    case "conversation.item.input_audio_transcription.text" -> {
                        // 源语言转写中间结果：text=已确认文本，stash=待确认
                        String confirmed = data.getStr("text", "");
                        String stash = data.getStr("stash", "");
                        String lang = data.getStr("language");
                        String itemId = data.getStr("item_id");
                        if (StringUtils.hasText(confirmed) || StringUtils.hasText(stash)) {
                            if (itemId != null) shownSourceItemIds.add(itemId);
                            listener.onSourceTranscript(new LiveTranslateResult(
                                    null, itemId, confirmed + stash, null, stash, false, lang));
                        }
                    }
                    case "conversation.item.input_audio_transcription.completed" -> {
                        // 源语言转写最终结果
                        String transcript = data.getStr("transcript", "");
                        String lang = data.getStr("language");
                        String itemId = data.getStr("item_id");
                        boolean shown = itemId != null && shownSourceItemIds.remove(itemId);
                        if (StringUtils.hasText(transcript)) {
                            // 正常最终结果
                            listener.onSourceTranscript(new LiveTranslateResult(
                                    null, itemId, transcript, null, null, true, lang));
                        } else if (shown) {
                            // 空 transcript 且已展示过：模型撤回误识别结果，通知前端移除
                            listener.onSourceTranscript(new LiveTranslateResult(
                                    null, itemId, "", null, null, true, null));
                        }
                        // 空 transcript 且从未展示过（纯静音/噪声）：跳过
                    }
                    case "response.audio_transcript.text" -> {
                        // 翻译文本流式（有音频输出模式）
                        String confirmed = data.getStr("text", "");
                        String stash = data.getStr("stash", "");
                        String responseId = data.getStr("response_id");
                        String itemId = data.getStr("item_id");
                        if (StringUtils.hasText(confirmed) || StringUtils.hasText(stash)) {
                            listener.onTranslation(new LiveTranslateResult(
                                    responseId, itemId, null, confirmed + stash, stash, false, null));
                        }
                    }
                    case "response.audio_transcript.done" -> {
                        // 翻译文本最终（有音频输出模式）
                        String transcript = data.getStr("transcript", "");
                        String responseId = data.getStr("response_id");
                        String itemId = data.getStr("item_id");
                        if (StringUtils.hasText(transcript)) {
                            listener.onTranslation(new LiveTranslateResult(
                                    responseId, itemId, null, transcript, null, true, null));
                        }
                    }
                    case "response.text.text" -> {
                        // 翻译文本流式（纯文本输出模式）
                        String confirmed = data.getStr("text", "");
                        String stash = data.getStr("stash", "");
                        String responseId = data.getStr("response_id");
                        String itemId = data.getStr("item_id");
                        if (StringUtils.hasText(confirmed) || StringUtils.hasText(stash)) {
                            listener.onTranslation(new LiveTranslateResult(
                                    responseId, itemId, null, confirmed + stash, stash, false, null));
                        }
                    }
                    case "response.text.done" -> {
                        // 翻译文本最终（纯文本输出模式）
                        String finalText = data.getStr("text", "");
                        String responseId = data.getStr("response_id");
                        String itemId = data.getStr("item_id");
                        if (StringUtils.hasText(finalText)) {
                            listener.onTranslation(new LiveTranslateResult(
                                    responseId, itemId, null, finalText, null, true, null));
                        }
                    }
                    case "response.audio.delta" -> {
                        String delta = data.getStr("delta");
                        String responseId = data.getStr("response_id");
                        if (StringUtils.hasText(delta)) {
                            listener.onAudioDelta(responseId, delta);
                        }
                    }
                    case "response.audio.done" -> {
                        String responseId = data.getStr("response_id");
                        listener.onAudioDone(responseId);
                    }
                    case "session.finished" -> listener.onCompleted();
                    case "error" -> {
                        JSONObject err = data.getJSONObject("error");
                        String msg = err != null ? err.getStr("message", "语音翻译错误")
                                : data.getStr("message", "语音翻译错误");
                        log.warn("livetranslate 错误事件: {}", text);
                        listener.onError(msg);
                    }
                    default -> log.debug("livetranslate 未处理事件: type={}", type);
                }
            } catch (Exception e) {
                log.error("解析 livetranslate 消息出错", e);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            log.error("livetranslate 连接失败", t);
            listener.onError("实时语音翻译连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("livetranslate 连接关闭: code={}, reason={}", code, reason);
            if (code == 1000) {
                listener.onCompleted();
            } else {
                listener.onError(StringUtils.hasText(reason) ? reason
                        : ("实时语音翻译连接异常关闭 (code=" + code + ")"));
            }
        }
    }
}
