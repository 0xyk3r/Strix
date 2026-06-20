package cn.projectan.strix.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.tts.TtsAudioListener;
import cn.projectan.strix.core.module.ai.tts.TtsParams;
import cn.projectan.strix.core.module.ai.tts.TtsProvider;
import cn.projectan.strix.core.module.ai.tts.TtsStreamSession;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.service.system.AiModelConfigService;
import cn.projectan.strix.service.system.DashScopeAiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI 双向流式语音合成（TTS）WebSocket 处理器（平台无关）。
 * <p>
 * 作为浏览器与各 TTS 平台之间的代理：浏览器流式上行文本，处理器按模型配置选择
 * {@link TtsProvider} 建立上游会话并转发文本，平台返回的音频帧经回调以二进制帧下发给浏览器。
 *
 * <p>客户端连接 URL：{@code ws://host/api/ws/ai/tts?token=<token>&configKey=<key>}</p>
 *
 * <p>客户端上行消息（文本帧 JSON）：</p>
 * <pre>
 *   {@code {"type":"config","voiceId":"...","params":{...}}}  // 首条：会话级参数（含 voiceId）
 *   {@code {"type":"text","text":"待合成文本片段"}}            // 流式追加文本
 *   {@code {"type":"finish"}}                                  // 文本发送完毕
 * </pre>
 *
 * <p>下发给客户端：音频以二进制帧下发；控制消息为文本帧 JSON：</p>
 * <pre>
 *   {@code {"type":"started"}}     // 上游任务已开始
 *   {@code {"done":true}}          // 合成完成
 *   {@code {"error":"错误信息"}}   // 错误
 * </pre>
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTtsWebSocketHandler extends AbstractWebSocketHandler {

    private static final int MAX_CONNECTIONS_PER_USER = 3;
    private static final long IDLE_TIMEOUT_MS = 120_000L;
    private static final long CONFIG_WAIT_TIMEOUT_MS = 1500L;

    private final AiModelConfigService aiModelConfigService;
    private final DashScopeAiService dashScopeAiService;
    private final List<TtsProvider> ttsProviders;

    @Qualifier("strixScheduledExecutor")
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * sessionId → 上游 TTS 会话
     */
    private final Map<String, TtsStreamSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActiveAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> userConnCount = new ConcurrentHashMap<>();
    /**
     * sessionId → 待开启上下文（已通过校验、等待前端 config 消息）。
     */
    private final Map<String, PendingOpen> pendingOpens = new ConcurrentHashMap<>();

    private record PendingOpen(WebSocketSession session, AiModelConfig config, TtsProvider provider) {
    }

    @PostConstruct
    void init() {
        scheduledExecutor.scheduleWithFixedDelay(this::sweepIdleSessions, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String configKey = (String) session.getAttributes().get("configKey");
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);

        if (config.getType() == null || config.getType() != AiModelType.TTS) {
            sendToClient(session, errorJson("该配置不是语音合成 (TTS) 模型"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        TtsProvider provider = ttsProviders.stream()
                .filter(p -> p.supports(config))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            sendToClient(session, errorJson("暂不支持该 TTS 模型: " + config.getModelName()));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        if (userConnCount.getOrDefault(userId, 0) >= MAX_CONNECTIONS_PER_USER) {
            sendToClient(session, errorJson("并发语音合成连接数已达上限，请稍后再试"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (userId != null) {
            userConnCount.merge(userId, 1, Integer::sum);
        }
        clientSessions.put(session.getId(), session);
        lastActiveAt.put(session.getId(), System.currentTimeMillis());

        pendingOpens.put(session.getId(), new PendingOpen(session, config, provider));
        scheduledExecutor.schedule(() -> openIfPending(session.getId(), null, null),
                CONFIG_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        log.info("TTS WebSocket 已建立(待配置): sessionId={}, userId={}, provider={}, model={}",
                session.getId(), userId, provider.getClass().getSimpleName(), config.getModelName());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception {
        lastActiveAt.put(session.getId(), System.currentTimeMillis());
        String payload = message.getPayload().trim();
        if (payload.isEmpty()) {
            return;
        }
        JSONObject msg;
        try {
            msg = JSONUtil.parseObj(payload);
        } catch (Exception e) {
            log.warn("解析 TTS 客户端消息失败: sessionId={}, payload={}", session.getId(), payload, e);
            return;
        }
        String type = msg.getStr("type");
        if ("config".equals(type)) {
            JSONObject params = msg.getJSONObject("params");
            String voiceId = msg.getStr("voiceId");
            String paramsJson = params != null ? params.toJSONString(0) : null;
            openIfPending(session.getId(), voiceId, paramsJson);
        } else if ("text".equals(type)) {
            TtsStreamSession ttsSession = sessions.get(session.getId());
            if (ttsSession != null) {
                ttsSession.sendText(msg.getStr("text"));
            }
        } else if ("finish".equals(type)) {
            TtsStreamSession ttsSession = sessions.get(session.getId());
            if (ttsSession != null) {
                ttsSession.finish();
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) throws Exception {
        cleanupSession(session);
        log.info("TTS WebSocket 已断开: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception) throws Exception {
        log.error("TTS WebSocket 传输错误: sessionId={}", session.getId(), exception);
        cleanupSession(session);
    }

    /**
     * 触发待开启会话的 open（幂等）：config 消息到达或超时兜底均调用此方法，仅首个生效。
     *
     * @param sessionId  客户端会话 ID
     * @param voiceId    音色 ID（可空，超时兜底传 null）
     * @param paramsJson 会话级覆盖参数 JSON（可空）
     */
    private void openIfPending(String sessionId, String voiceId, String paramsJson) {
        PendingOpen pending = pendingOpens.remove(sessionId);
        if (pending == null) {
            return;
        }
        WebSocketSession session = pending.session();
        AiModelConfig config = pending.config();
        TtsParams merged = dashScopeAiService.mergeTtsParams(config, voiceId, paramsJson);
        if (merged.voice() == null || merged.voice().isBlank()) {
            sendToClient(session, errorJson("缺少音色（voice），请先选择或注册音色后再合成"));
            closeClient(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        TtsStreamSession ttsSession = pending.provider().openStream(config, merged, new TtsAudioListener() {
            @Override
            public void onAudio(byte[] audio) {
                sendBinaryToClient(session, audio);
            }

            @Override
            public void onError(String message) {
                sendToClient(session, errorJson(message));
                closeClient(session, CloseStatus.SERVER_ERROR);
            }

            @Override
            public void onCompleted() {
                sendToClient(session, JSONUtil.createObj().set("done", true).toJSONString(0));
            }
        });
        sessions.put(sessionId, ttsSession);
        sendToClient(session, JSONUtil.createObj().set("type", "started").toJSONString(0));
        log.info("TTS 上游会话已建立: sessionId={}, model={}", sessionId, config.getModelName());
    }

    private void cleanupSession(WebSocketSession session) {
        String sessionId = session.getId();
        pendingOpens.remove(sessionId);
        boolean tracked = clientSessions.remove(sessionId) != null;
        lastActiveAt.remove(sessionId);
        if (tracked) {
            String userId = (String) session.getAttributes().get("userId");
            if (userId != null) {
                userConnCount.computeIfPresent(userId, (k, v) -> v <= 1 ? null : v - 1);
            }
        }
        TtsStreamSession ttsSession = sessions.remove(sessionId);
        if (ttsSession != null) {
            ttsSession.close();
        }
    }

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

    private void closeClient(WebSocketSession session, CloseStatus status) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.close(status);
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
                    log.warn("向客户端发送 TTS 控制消息失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }

    private static void sendBinaryToClient(WebSocketSession session, byte[] audio) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new BinaryMessage(ByteBuffer.wrap(audio)));
                } catch (IOException e) {
                    log.warn("向客户端发送 TTS 音频帧失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }
}

