package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.*;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 阿里云百炼 DashScope run-task / finish-task 协议实时识别 Provider 基类。
 * <p>
 * 承载 Paraformer 与 Fun-ASR 共用的协议逻辑：
 * <ul>
 *   <li>连接 {@code wss://dashscope.aliyuncs.com/api-ws/v1/inference}</li>
 *   <li>建连后发送 {@code run-task}（parameters 由子类 {@link #buildParameters} 定制）</li>
 *   <li>音频以二进制帧上行（task-started 前缓存）</li>
 *   <li>{@code result-generated} 返回句级/字级时间戳与（可选）情感，{@code task-finished} 表示结束</li>
 * </ul>
 * 子类只需定制 {@link #supports}、{@link #buildParameters}、{@link #supportsEmotion}。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Slf4j
public abstract class AbstractDashScopeRunTaskAsrProvider implements RealtimeAsrProvider {

    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    private static final int MAX_PENDING_FRAMES = 200;

    protected final DashScopeHttpClient dashScopeHttpClient;

    protected AbstractDashScopeRunTaskAsrProvider(DashScopeHttpClient dashScopeHttpClient) {
        this.dashScopeHttpClient = dashScopeHttpClient;
    }

    /**
     * 子类拼装 run-task 的 parameters（各自支持的字段集）。基类已注入 format=pcm、sample_rate=16000，子类无需重复。
     */
    protected abstract JSONObject buildParameters(AiModelConfig config, AsrSessionParams params);

    /**
     * 该模型是否支持情感识别（仅 paraformer-realtime-8k-v2）。默认 false。
     */
    protected boolean supportsEmotion(AiModelConfig config) {
        return false;
    }

    @Override
    public RealtimeAsrSession open(AiModelConfig config, AsrSessionParams params, AsrResultListener listener) {
        String base = config.getBaseUrl();
        String url = (base != null && (base.startsWith("ws://") || base.startsWith("wss://"))) ? base : WS_URL;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();

        JSONObject parameters = buildParameters(config, params == null ? AsrSessionParams.empty() : params)
                .set("format", "pcm")
                .set("sample_rate", 16000);
        boolean emotion = supportsEmotion(config);

        RunTaskSession session = new RunTaskSession(config.getModelName(), parameters, emotion, listener);
        WebSocket ws = dashScopeHttpClient.getHttpClient().newWebSocket(request, session);
        session.attach(ws);
        return session;
    }

    /**
     * 解析 result-generated 的 sentence 对象为 AsrTranscript（含句级/字级时间戳与情感）。
     * 静态方法以便单测。情感仅在 supportsEmotion && sentence_end 时解析。
     *
     * @param sentence        payload.output.sentence 对象
     * @param itemId          稳定句 ID（taskId-序号）
     * @param supportsEmotion 该模型是否支持情感（paraformer-8k-v2）
     */
    static AsrTranscript parseSentence(JSONObject sentence, String itemId, boolean supportsEmotion) {
        String text = sentence.getStr("text", "");
        boolean isFinal = sentence.getBool("sentence_end", false);
        Long beginTime = sentence.getLong("begin_time", null);
        Long endTime = sentence.getLong("end_time", null);

        List<AsrWord> words = null;
        JSONArray wordsArr = sentence.getJSONArray("words");
        if (wordsArr != null && !wordsArr.isEmpty()) {
            words = new ArrayList<>(wordsArr.size());
            for (Object o : wordsArr) {
                JSONObject w = (JSONObject) o;
                words.add(new AsrWord(
                        w.getLong("begin_time", null),
                        w.getLong("end_time", null),
                        w.getStr("text", ""),
                        w.getStr("punctuation", "")));
            }
        }

        String emotion = null;
        String emotionScheme = null;
        Double emoConfidence = null;
        if (supportsEmotion && isFinal) {
            String emo = sentence.getStr("emo_tag", null);
            if (emo != null && !emo.isBlank()) {
                emotion = emo;
                emotionScheme = "polarity3";
                emoConfidence = sentence.getDouble("emo_confidence", null);
            }
        }

        return new AsrTranscript(itemId, text, isFinal, emotion, emotionScheme,
                emoConfidence, null, beginTime, endTime, words);
    }
    // PLACEHOLDER_SESSION

    /**
     * run-task 会话：既是上行 RealtimeAsrSession 也是下行 WebSocketListener。
     */
    private static class RunTaskSession extends WebSocketListener implements RealtimeAsrSession {

        private final String model;
        private final JSONObject parameters;
        private final boolean supportsEmotion;
        private final String taskId;
        private final AsrResultListener listener;
        private volatile WebSocket ws;
        private volatile boolean taskReady = false;
        private final ConcurrentLinkedQueue<byte[]> pendingAudio = new ConcurrentLinkedQueue<>();
        /**
         * 句序号：协议不返回 item_id，用 taskId + 序号生成稳定句 ID，供前端按句聚合。仅下行回调线程访问。
         */
        private int sentenceSeq = 0;

        RunTaskSession(String model, JSONObject parameters, boolean supportsEmotion, AsrResultListener listener) {
            this.model = model;
            this.parameters = parameters;
            this.supportsEmotion = supportsEmotion;
            this.listener = listener;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        void attach(WebSocket ws) {
            this.ws = ws;
        }

        @Override
        public void sendAudio(byte[] pcm) {
            WebSocket w = ws;
            if (w == null || pcm == null || pcm.length == 0) return;
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
                            .set("parameters", parameters)
                            .set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            webSocket.send(runTaskMsg);
            log.info("run-task ASR 已发送: taskId={}, model={}", taskId, model);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject msg = JSONUtil.parseObj(text);
                JSONObject header = msg.getJSONObject("header");
                String event = header != null ? header.getStr("event") : null;

                if ("task-started".equals(event)) {
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
                        String itemId = taskId + "-" + sentenceSeq;
                        AsrTranscript t = parseSentence(sentence, itemId, supportsEmotion);
                        listener.onTranscript(t);
                        if (t.isFinal()) {
                            sentenceSeq++;
                        }
                    }
                } else if ("task-finished".equals(event)) {
                    listener.onCompleted();
                } else if ("task-failed".equals(event)) {
                    String errMsg = header != null ? header.getStr("error_message", "识别任务失败") : "识别任务失败";
                    listener.onError(errMsg);
                }
            } catch (Exception e) {
                log.error("解析 run-task ASR 消息出错", e);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            log.error("run-task ASR 连接失败: taskId={}", taskId, t);
            listener.onError("实时识别连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("run-task ASR 连接关闭: taskId={}, code={}, reason={}", taskId, code, reason);
        }
    }
}
