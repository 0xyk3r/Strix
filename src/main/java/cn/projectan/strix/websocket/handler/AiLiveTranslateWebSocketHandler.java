package cn.projectan.strix.websocket.handler;

import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.livetranslate.*;
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
 * AI 实时语音翻译（LiveTranslate）WebSocket 处理器（平台无关）。
 * <p>
 * 作为浏览器与各翻译平台之间的代理：浏览器以二进制帧上行 PCM 16kHz 单声道音频，
 * 处理器按模型配置选择 {@link RealtimeLiveTranslateProvider} 建立上游会话并转发音频，
 * 平台返回的结果经回调转为统一 JSON 下发给浏览器。
 *
 * <p>客户端连接 URL：{@code ws://host/api/ws/ai/live-translate?token=<token>&configKey=<key>}</p>
 *
 * <p>下发给客户端的 JSON：</p>
 * <pre>
 *   源语言转写流式：{"type":"source","itemId":"...","text":"...","stash":"...","final":false,"language":"zh"}
 *   源语言转写最终：{"type":"source","itemId":"...","text":"...","final":true,"language":"zh"}
 *   翻译文本流式：  {"type":"translation","responseId":"...","text":"...","stash":"...","final":false}
 *   翻译文本最终：  {"type":"translation","responseId":"...","text":"...","final":true}
 *   翻译音频增量：  {"type":"audio","responseId":"...","delta":"base64..."}
 *   翻译音频结束：  {"type":"audioDone","responseId":"..."}
 *   会话结束：      {"done":true}
 *   错误：          {"error":"错误信息"}
 * </pre>
 *
 * <p><b>音频要求</b>：PCM 16kHz 单声道 16-bit（little-endian）</p>
 *
 * @author ProjectAn
 * @since 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiLiveTranslateWebSocketHandler extends AbstractWebSocketHandler {

    private static final int MAX_CONNECTIONS_PER_USER = 3;
    private static final long IDLE_TIMEOUT_MS = 120_000L;
    private static final long CONFIG_WAIT_TIMEOUT_MS = 1500L;

    private final AiModelConfigService aiModelConfigService;
    private final List<RealtimeLiveTranslateProvider> translateProviders;

    @Qualifier("strixScheduledExecutor")
    private final ScheduledExecutorService scheduledExecutor;

    private final Map<String, RealtimeLiveTranslateSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActiveAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> userConnCount = new ConcurrentHashMap<>();
    private final Map<String, PendingOpen> pendingOpens = new ConcurrentHashMap<>();

    private record PendingOpen(WebSocketSession session, AiModelConfig config,
                               RealtimeLiveTranslateProvider provider) {
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

        if (config.getType() == null || config.getType() != AiModelType.LIVE_TRANSLATE) {
            sendToClient(session, errorJson("该配置不是实时语音翻译 (LiveTranslate) 模型"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        RealtimeLiveTranslateProvider provider = translateProviders.stream()
                .filter(p -> p.supports(config))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            sendToClient(session, errorJson("暂不支持该翻译模型: " + config.getModelName()));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        if (userConnCount.getOrDefault(userId, 0) >= MAX_CONNECTIONS_PER_USER) {
            sendToClient(session, errorJson("并发语音翻译连接数已达上限，请稍后再试"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (userId != null) {
            userConnCount.merge(userId, 1, Integer::sum);
        }
        clientSessions.put(session.getId(), session);
        lastActiveAt.put(session.getId(), System.currentTimeMillis());

        pendingOpens.put(session.getId(), new PendingOpen(session, config, provider));
        scheduledExecutor.schedule(() -> openIfPending(session.getId(), null),
                CONFIG_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        log.info("LiveTranslate WebSocket 已建立(待配置): sessionId={}, userId={}, provider={}, model={}",
                session.getId(), userId, provider.getClass().getSimpleName(), config.getModelName());
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session,
                                       @NonNull BinaryMessage message) throws Exception {
        lastActiveAt.put(session.getId(), System.currentTimeMillis());
        RealtimeLiveTranslateSession ltSession = sessions.get(session.getId());
        if (ltSession != null) {
            java.nio.ByteBuffer payload = message.getPayload();
            byte[] pcm = new byte[payload.remaining()];
            payload.get(pcm);
            ltSession.sendAudio(pcm);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        if ("end".equalsIgnoreCase(payload)) {
            RealtimeLiveTranslateSession ltSession = sessions.get(session.getId());
            if (ltSession != null) {
                ltSession.finish();
            }
            return;
        }
        try {
            cn.hutool.json.JSONObject msg = JSONUtil.parseObj(payload);
            if ("config".equals(msg.getStr("type"))) {
                cn.hutool.json.JSONObject params = msg.getJSONObject("params");
                LiveTranslateSessionParams override = params != null
                        ? LiveTranslateSessionParams.fromJson(params.toJSONString(0)) : null;
                openIfPending(session.getId(), override);
            }
        } catch (Exception e) {
            log.warn("解析 LiveTranslate config 消息失败: sessionId={}, payload={}", session.getId(), payload, e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) throws Exception {
        cleanupSession(session);
        log.info("LiveTranslate WebSocket 已断开: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception) throws Exception {
        log.error("LiveTranslate WebSocket 传输错误: sessionId={}", session.getId(), exception);
        cleanupSession(session);
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
        RealtimeLiveTranslateSession ltSession = sessions.remove(sessionId);
        if (ltSession != null) {
            ltSession.close();
        }
    }

    private void openIfPending(String sessionId, LiveTranslateSessionParams overrideParams) {
        PendingOpen pending = pendingOpens.remove(sessionId);
        if (pending == null) return;

        WebSocketSession session = pending.session();
        AiModelConfig config = pending.config();
        LiveTranslateSessionParams merged = LiveTranslateSessionParams.fromJson(config.getLiveTranslateParams())
                .merge(overrideParams);

        RealtimeLiveTranslateSession ltSession = pending.provider().open(config, merged,
                new LiveTranslateResultListener() {
                    @Override
                    public void onSourceTranscript(LiveTranslateResult result) {
                        sendToClient(session, buildSourceJson(result));
                    }

                    @Override
                    public void onTranslation(LiveTranslateResult result) {
                        sendToClient(session, buildTranslationJson(result));
                    }

                    @Override
                    public void onAudioDelta(String responseId, String audioDelta) {
                        sendToClient(session, buildAudioDeltaJson(responseId, audioDelta));
                    }

                    @Override
                    public void onAudioDone(String responseId) {
                        sendToClient(session, buildAudioDoneJson(responseId));
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
        sessions.put(sessionId, ltSession);
        log.info("LiveTranslate 上游会话已建立: sessionId={}, model={}", sessionId, config.getModelName());
    }

    private static String buildSourceJson(LiveTranslateResult r) {
        cn.hutool.json.JSONObject obj = JSONUtil.createObj()
                .set("type", "source")
                .set("itemId", r.itemId())
                .set("text", r.sourceText())
                .set("final", r.isFinal());
        if (r.removed()) {
            obj.set("removed", true);
        }
        if (StringUtils.hasText(r.stash())) {
            obj.set("stash", r.stash());
        }
        if (StringUtils.hasText(r.sourceLanguage())) {
            obj.set("language", r.sourceLanguage());
        }
        return obj.toJSONString(0);
    }

    private static String buildTranslationJson(LiveTranslateResult r) {
        cn.hutool.json.JSONObject obj = JSONUtil.createObj()
                .set("type", "translation")
                .set("responseId", r.responseId())
                .set("text", r.translationText())
                .set("final", r.isFinal());
        if (StringUtils.hasText(r.stash())) {
            obj.set("stash", r.stash());
        }
        if (StringUtils.hasText(r.itemId())) {
            obj.set("itemId", r.itemId());
        }
        return obj.toJSONString(0);
    }

    private static String buildAudioDeltaJson(String responseId, String delta) {
        return JSONUtil.createObj()
                .set("type", "audio")
                .set("responseId", responseId)
                .set("delta", delta)
                .toJSONString(0);
    }

    private static String buildAudioDoneJson(String responseId) {
        return JSONUtil.createObj()
                .set("type", "audioDone")
                .set("responseId", responseId)
                .toJSONString(0);
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
                    log.warn("向客户端发送 LiveTranslate 消息失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }
}
