package cn.projectan.strix.websocket.handler;

import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.AsrResultListener;
import cn.projectan.strix.core.module.ai.asr.AsrTranscript;
import cn.projectan.strix.core.module.ai.asr.RealtimeAsrProvider;
import cn.projectan.strix.core.module.ai.asr.RealtimeAsrSession;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.service.system.AiModelConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI 实时语音识别（ASR）WebSocket 处理器（平台无关）。
 * <p>
 * 作为浏览器与各 ASR 平台之间的代理：浏览器以二进制帧上行 PCM 16kHz 单声道音频，
 * 处理器按模型配置选择 {@link RealtimeAsrProvider} 建立上游会话并转发音频，
 * 平台返回的增量/最终结果经回调转为统一 JSON 下发给浏览器。
 *
 * <p>客户端连接 URL：{@code ws://host/api/ws/ai/asr?token=<token>&configKey=<key>}</p>
 *
 * <p>下发给客户端的 JSON：</p>
 * <pre>
 *   {@code {"itemId":"item_x","text":"识别文本","final":false,"emotion":"neutral","language":"zh"}}  // 中间结果
 *   {@code {"itemId":"item_x","text":"整句","final":true,"emotion":"neutral","language":"zh"}}        // 句子完成
 *   {@code {"done":true}}                        // 任务结束
 *   {@code {"error":"错误信息"}}                 // 错误
 * </pre>
 * <p>其中 {@code emotion} / {@code language} 仅在平台返回时下发（如 Qwen-ASR），不支持的平台省略该键。</p>
 *
 * <p><b>音频要求</b>：PCM 16kHz 单声道 16-bit（little-endian）</p>
 * <p><b>加固</b>：仅允许 ASR 类型配置；每用户并发连接数上限；空闲超时自动关闭。</p>
 *
 * @author ProjectAn
 * @since 2026-05-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAsrWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * 每个用户允许的最大并发 ASR 连接数
     */
    private static final int MAX_CONNECTIONS_PER_USER = 3;
    /**
     * 空闲超时（毫秒）：超过此时长未收到音频帧的连接将被关闭
     */
    private static final long IDLE_TIMEOUT_MS = 120_000L;

    private final AiModelConfigService aiModelConfigService;
    /**
     * 所有已注册的 ASR 平台 Provider（Spring 注入）
     */
    private final List<RealtimeAsrProvider> asrProviders;

    @Qualifier("strixScheduledExecutor")
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * sessionId → 上游 ASR 会话
     */
    private final Map<String, RealtimeAsrSession> sessions = new ConcurrentHashMap<>();
    /**
     * sessionId → 客户端会话
     */
    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    /**
     * sessionId → 最近一次收到音频帧的时间戳（毫秒）
     */
    private final Map<String, Long> lastActiveAt = new ConcurrentHashMap<>();
    /**
     * userId → 当前并发连接数
     */
    private final Map<String, Integer> userConnCount = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        scheduledExecutor.scheduleWithFixedDelay(this::sweepIdleSessions, 30, 30, TimeUnit.SECONDS);
    }

    // ============================================================
    //  Spring WebSocket 生命周期
    // ============================================================

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String configKey = (String) session.getAttributes().get("configKey");
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);

        // 仅允许实时语音识别（ASR）类型配置
        if (config.getType() == null || config.getType() != AiModelType.ASR) {
            sendToClient(session, errorJson("该配置不是实时语音识别 (ASR) 模型"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 选择支持该模型的 Provider
        RealtimeAsrProvider provider = asrProviders.stream()
                .filter(p -> p.supports(config))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            sendToClient(session, errorJson("暂不支持该 ASR 模型: " + config.getModelName()));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 每用户并发连接数上限（软上限）
        if (userConnCount.getOrDefault(userId, 0) >= MAX_CONNECTIONS_PER_USER) {
            sendToClient(session, errorJson("并发语音识别连接数已达上限，请稍后再试"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (userId != null) {
            userConnCount.merge(userId, 1, Integer::sum);
        }
        clientSessions.put(session.getId(), session);
        lastActiveAt.put(session.getId(), System.currentTimeMillis());

        // 建立上游会话，结果回调转发给浏览器
        RealtimeAsrSession asrSession = provider.open(config, new AsrResultListener() {
            @Override
            public void onTranscript(AsrTranscript result) {
                log.info("qwen-asr-realtime 收到识别结果: sessionId={}, itemId={}, final={}, text={}",
                        session.getId(), result.itemId(), result.isFinal(), result.text());

                cn.hutool.json.JSONObject obj = JSONUtil.createObj()
                        .set("itemId", result.itemId())
                        .set("text", result.text())
                        .set("final", result.isFinal());
                // emotion / language 仅在平台返回时下发（Qwen-ASR 支持，Paraformer 为 null）
                if (StringUtils.hasText(result.emotion())) {
                    obj.set("emotion", result.emotion());
                }
                if (StringUtils.hasText(result.language())) {
                    obj.set("language", result.language());
                }
                sendToClient(session, obj.toJSONString(0));
            }

            @Override
            public void onError(String message) {
                sendToClient(session, errorJson(message));
                closeClient(session);
            }

            @Override
            public void onCompleted() {
                sendToClient(session, JSONUtil.createObj().set("done", true).toJSONString(0));
            }
        });
        sessions.put(session.getId(), asrSession);

        log.info("ASR WebSocket 已建立: sessionId={}, userId={}, provider={}, model={}",
                session.getId(), userId, provider.getClass().getSimpleName(), config.getModelName());
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session,
                                       @NonNull BinaryMessage message) throws Exception {
        lastActiveAt.put(session.getId(), System.currentTimeMillis());
        RealtimeAsrSession asrSession = sessions.get(session.getId());
        if (asrSession != null) {
            // 基于 ByteBuffer 的 remaining() 精确取字节，避免 array() 取整个底层数组带来多余字节
            java.nio.ByteBuffer payload = message.getPayload();
            byte[] pcm = new byte[payload.remaining()];
            payload.get(pcm);
            asrSession.sendAudio(pcm);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception {
        // 客户端发送 "end" 表示音频流结束
        if ("end".equalsIgnoreCase(message.getPayload().trim())) {
            RealtimeAsrSession asrSession = sessions.get(session.getId());
            if (asrSession != null) {
                asrSession.finish();
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) throws Exception {
        cleanupSession(session);
        log.info("ASR WebSocket 已断开: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception) throws Exception {
        log.error("ASR WebSocket 传输错误: sessionId={}", session.getId(), exception);
        cleanupSession(session);
    }

    // ============================================================
    //  内部工具
    // ============================================================

    private void cleanupSession(WebSocketSession session) {
        String sessionId = session.getId();
        boolean tracked = clientSessions.remove(sessionId) != null;
        lastActiveAt.remove(sessionId);
        if (tracked) {
            String userId = (String) session.getAttributes().get("userId");
            if (userId != null) {
                userConnCount.computeIfPresent(userId, (k, v) -> v <= 1 ? null : v - 1);
            }
        }
        RealtimeAsrSession asrSession = sessions.remove(sessionId);
        if (asrSession != null) {
            asrSession.close();
        }
    }

    /**
     * 周期性关闭空闲超时的连接（无音频帧超过 {@link #IDLE_TIMEOUT_MS}）
     */
    private void sweepIdleSessions() {
        long now = System.currentTimeMillis();
        lastActiveAt.forEach((sessionId, last) -> {
            if (now - last > IDLE_TIMEOUT_MS) {
                WebSocketSession s = clientSessions.get(sessionId);
                if (s != null) {
                    synchronized (s) {
                        if (s.isOpen()) {
                            try {
                                s.close(CloseStatus.GOING_AWAY.withReason("idle timeout"));
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            }
        });
    }

    private void closeClient(WebSocketSession session) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String errorJson(String message) {
        return JSONUtil.createObj().set("error", message).toJSONString(0);
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
}
