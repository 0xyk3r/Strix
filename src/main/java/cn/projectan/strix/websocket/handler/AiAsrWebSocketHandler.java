package cn.projectan.strix.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.service.system.AiModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 实时 ASR WebSocket 处理器
 * <p>
 * 作为 DashScope WebSocket 实时语音识别的代理：
 * <ol>
 *   <li>客户端连接时，向 DashScope 建立 WebSocket 并发送 {@code run-task} 启动识别</li>
 *   <li>客户端发送 PCM 16kHz 单声道音频二进制帧 → 转发至 DashScope</li>
 *   <li>DashScope 推送识别结果 → 解析后以 JSON 文本消息转发给客户端</li>
 *   <li>客户端发送文本 {@code end} 或断开连接 → 发送 {@code finish-task} 结束任务</li>
 * </ol>
 *
 * <p>客户端连接 URL：{@code ws://host/ws/ai/asr?token=<token>&configKey=<key>}</p>
 *
 * <p>客户端收到的 JSON 格式：</p>
 * <pre>
 *   {@code {"text":"识别文本","sentenceId":1,"final":false}}  // 中间结果
 *   {@code {"text":"完整句子","sentenceId":2,"final":true}}   // 句子完成
 *   {@code {"done":true}}                                     // 任务结束
 *   {@code {"error":"错误信息"}}                              // 错误
 * </pre>
 *
 * <p><b>音频要求</b>：PCM 16kHz 单声道 16-bit（little-endian）</p>
 *
 * @author ProjectAn
 * @since 2026-05-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAsrWebSocketHandler extends AbstractWebSocketHandler {

    private static final String DASHSCOPE_WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";

    private final AiModelConfigService aiModelConfigService;
    private final DashScopeHttpClient dashScopeHttpClient;

    /**
     * sessionId → DashScope WebSocket
     */
    private final Map<String, WebSocket> dashScopeWsMap = new ConcurrentHashMap<>();
    /**
     * sessionId → taskId
     */
    private final Map<String, String> taskIdMap = new ConcurrentHashMap<>();

    // ============================================================
    //  Spring WebSocket 生命周期
    // ============================================================

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String configKey = (String) session.getAttributes().get("configKey");
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskIdMap.put(session.getId(), taskId);

        Request wsRequest = new Request.Builder()
                .url(DASHSCOPE_WS_URL)
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();

        String model = config.getModelName();
        WebSocket dashScopeWs = dashScopeHttpClient.getHttpClient()
                .newWebSocket(wsRequest, new DashScopeAsrListener(session, taskId, model));
        dashScopeWsMap.put(session.getId(), dashScopeWs);

        log.info("ASR WebSocket 已建立: sessionId={}, configKey={}, taskId={}",
                session.getId(), configKey, taskId);
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session,
                                       @NonNull BinaryMessage message) throws Exception {
        WebSocket dashScopeWs = dashScopeWsMap.get(session.getId());
        if (dashScopeWs != null) {
            byte[] audioBytes = message.getPayload().array();
            dashScopeWs.send(ByteString.of(audioBytes));
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception {
        // 客户端发送 "end" 信号表示音频流已结束
        if ("end".equalsIgnoreCase(message.getPayload().trim())) {
            sendFinishTask(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) throws Exception {
        sendFinishTask(session.getId());
        dashScopeWsMap.remove(session.getId());
        taskIdMap.remove(session.getId());
        log.info("ASR WebSocket 已断开: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception) throws Exception {
        log.error("ASR WebSocket 传输错误: sessionId={}", session.getId(), exception);
        sendFinishTask(session.getId());
        dashScopeWsMap.remove(session.getId());
        taskIdMap.remove(session.getId());
    }

    // ============================================================
    //  内部工具
    // ============================================================

    private void sendFinishTask(String sessionId) {
        WebSocket dashScopeWs = dashScopeWsMap.get(sessionId);
        String taskId = taskIdMap.get(sessionId);
        if (dashScopeWs != null && taskId != null) {
            String finishMsg = JSONUtil.createObj()
                    .set("header", JSONUtil.createObj()
                            .set("action", "finish-task")
                            .set("task_id", taskId)
                            .set("streaming", "duplex"))
                    .set("payload", JSONUtil.createObj()
                            .set("input", JSONUtil.createObj()))
                    .toJSONString(0);
            dashScopeWs.send(finishMsg);
            log.debug("已发送 finish-task: sessionId={}, taskId={}", sessionId, taskId);
        }
    }

    private static void sendToClient(WebSocketSession session, String jsonMsg) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMsg));
                } catch (IOException e) {
                    log.warn("向客户端发送 ASR 消息失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }

    // ============================================================
    //  DashScope WebSocket 监听器
    // ============================================================

    private class DashScopeAsrListener extends WebSocketListener {

        private final WebSocketSession clientSession;
        private final String taskId;
        private final String model;

        DashScopeAsrListener(WebSocketSession clientSession, String taskId, String model) {
            this.clientSession = clientSession;
            this.taskId = taskId;
            this.model = model;
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            // 发送 run-task 消息启动识别
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
            log.info("DashScope ASR run-task 已发送: taskId={}, model={}", taskId, model);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject msg = JSONUtil.parseObj(text);
                JSONObject header = msg.getJSONObject("header");
                String event = header != null ? header.getStr("event") : null;

                if ("result-generated".equals(event)) {
                    JSONObject payload = msg.getJSONObject("payload");
                    if (payload != null) {
                        JSONObject output = payload.getJSONObject("output");
                        if (output != null) {
                            JSONObject sentence = output.getJSONObject("sentence");
                            if (sentence != null) {
                                String recognizedText = sentence.getStr("text", "");
                                int sentenceId = sentence.getInt("sentence_id", 0);
                                boolean isFinal = sentence.getBool("is_end", false);

                                String clientMsg = JSONUtil.createObj()
                                        .set("text", recognizedText)
                                        .set("sentenceId", sentenceId)
                                        .set("final", isFinal)
                                        .toJSONString(0);
                                sendToClient(clientSession, clientMsg);
                            }
                        }
                    }
                } else if ("task-finished".equals(event)) {
                    sendToClient(clientSession, JSONUtil.createObj()
                            .set("done", true).toJSONString(0));
                }
            } catch (Exception e) {
                log.error("处理 DashScope ASR 消息出错: sessionId={}", clientSession.getId(), e);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t,
                              Response response) {
            log.error("DashScope ASR WebSocket 连接失败: sessionId={}, taskId={}",
                    clientSession.getId(), taskId, t);
            sendToClient(clientSession, JSONUtil.createObj()
                    .set("error", "DashScope ASR 连接失败: " + t.getMessage())
                    .toJSONString(0));
            synchronized (clientSession) {
                if (clientSession.isOpen()) {
                    try {
                        clientSession.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException ignored) {
                    }
                }
            }
            dashScopeWsMap.remove(clientSession.getId());
            taskIdMap.remove(clientSession.getId());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            log.info("DashScope ASR WebSocket 已关闭: sessionId={}, code={}, reason={}",
                    clientSession.getId(), code, reason);
            dashScopeWsMap.remove(clientSession.getId());
            taskIdMap.remove(clientSession.getId());
        }
    }
}
