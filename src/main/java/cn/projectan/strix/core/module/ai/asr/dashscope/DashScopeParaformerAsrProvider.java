package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.AsrResultListener;
import cn.projectan.strix.core.module.ai.asr.RealtimeAsrProvider;
import cn.projectan.strix.core.module.ai.asr.RealtimeAsrSession;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
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
 * 阿里云百炼 paraformer-realtime / gummy-realtime 系列实时识别 Provider。
 * <p>
 * 采用 DashScope 原生 {@code run-task / finish-task} 协议：
 * <ul>
 *   <li>连接 {@code wss://dashscope.aliyuncs.com/api-ws/v1/inference/}</li>
 *   <li>建连后发送 {@code run-task}（task_group=audio, function=recognition, pcm/16k）</li>
 *   <li>音频以二进制帧直接上行</li>
 *   <li>结果通过 {@code result-generated} 事件返回，{@code task-finished} 表示结束</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
@Component
@RequiredArgsConstructor
public class DashScopeParaformerAsrProvider implements RealtimeAsrProvider {

    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";

    private final DashScopeHttpClient dashScopeHttpClient;

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("paraformer") || model.contains("gummy");
    }

    @Override
    public RealtimeAsrSession open(AiModelConfig config, AsrResultListener listener) {
        String base = config.getBaseUrl();
        String url = (base != null && (base.startsWith("ws://") || base.startsWith("wss://"))) ? base : WS_URL;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();

        ParaformerSession session = new ParaformerSession(config.getModelName(), listener);
        WebSocket ws = dashScopeHttpClient.getHttpClient().newWebSocket(request, session);
        session.attach(ws);
        return session;
    }

    /**
     * paraformer/gummy 会话：run-task/finish-task 协议。
     */
    @Slf4j
    private static class ParaformerSession extends WebSocketListener implements RealtimeAsrSession {

        private final String model;
        private final String taskId;
        private final AsrResultListener listener;
        private volatile WebSocket ws;
        /**
         * run-task 是否已被服务端确认（收到 task-started）；之前的音频需缓存，保证 run-task 为首条消息
         */
        private volatile boolean taskReady = false;
        private final java.util.concurrent.ConcurrentLinkedQueue<byte[]> pendingAudio = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private static final int MAX_PENDING_FRAMES = 200;

        ParaformerSession(String model, AsrResultListener listener) {
            this.model = model;
            this.listener = listener;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        void attach(WebSocket ws) {
            this.ws = ws;
        }

        // ——— 上行 ———

        @Override
        public void sendAudio(byte[] pcm) {
            WebSocket w = ws;
            if (w == null || pcm == null || pcm.length == 0) return;
            // task-started 前先缓存音频，保证 run-task 为首条消息
            if (!taskReady) {
                if (pendingAudio.size() < MAX_PENDING_FRAMES) {
                    pendingAudio.add(pcm);
                }
                return;
            }
            w.send(ByteString.of(pcm));
        }

        @Override
        public void finish() {
            WebSocket w = ws;
            if (w == null) return;
            String finishMsg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "finish-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj().set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            w.send(finishMsg);
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
            String runTaskMsg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "run-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj()
                            .set("task_group", "audio")
                            .set("task", "asr")
                            .set("function", "recognition")
                            .set("model", model)
                            .set("parameters", JSONUtil.createObj()
                                    .set("format", "pcm")
                                    .set("sample_rate", 16000))
                            .set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            webSocket.send(runTaskMsg);
            log.info("paraformer ASR run-task 已发送: taskId={}, model={}", taskId, model);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject msg = JSONUtil.parseObj(text);
                JSONObject header = msg.getJSONObject("header");
                String event = header != null ? header.getStr("event") : null;

                if ("task-started".equals(event)) {
                    // 服务端已就绪，放行（并补发）缓存的音频
                    taskReady = true;
                    WebSocket w = ws;
                    if (w != null) {
                        byte[] p;
                        while ((p = pendingAudio.poll()) != null) {
                            w.send(ByteString.of(p));
                        }
                    }
                } else if ("result-generated".equals(event)) {
                    JSONObject payload = msg.getJSONObject("payload");
                    JSONObject output = payload != null ? payload.getJSONObject("output") : null;
                    JSONObject sentence = output != null ? output.getJSONObject("sentence") : null;
                    if (sentence != null) {
                        String recognizedText = sentence.getStr("text", "");
                        boolean isFinal = sentence.getBool("is_end", false);
                        listener.onTranscript(recognizedText, isFinal);
                    }
                } else if ("task-finished".equals(event)) {
                    listener.onCompleted();
                } else if ("task-failed".equals(event)) {
                    String errMsg = header != null ? header.getStr("error_message", "识别任务失败") : "识别任务失败";
                    listener.onError(errMsg);
                }
            } catch (Exception e) {
                log.error("解析 paraformer ASR 消息出错", e);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            log.error("paraformer ASR 连接失败: taskId={}", taskId, t);
            listener.onError("实时识别连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("paraformer ASR 连接关闭: taskId={}, code={}, reason={}", taskId, code, reason);
        }
    }
}
