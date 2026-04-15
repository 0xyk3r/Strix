package cn.projectan.strix.core.sse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * SSE 会话管理器
 * <p>
 * 管理所有 Manager 的 SSE 连接, 支持多标签页 (同一 managerId 多个 SseEmitter)
 * 内置 30 秒心跳保活机制
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Slf4j
@Component
public class SseSessionManager {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 分钟
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private ScheduledExecutorService heartbeatExecutor;

    @PostConstruct
    private void init() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("SSE 心跳调度已启动, 间隔 {}s", HEARTBEAT_INTERVAL_SECONDS);
    }

    @PreDestroy
    private void shutdown() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        emitters.values().forEach(list -> list.forEach(SseEmitter::complete));
        emitters.clear();
        log.info("SSE 会话管理器已关闭");
    }

    /**
     * 创建 SSE 连接
     *
     * @param managerId 管理员 ID
     * @return SseEmitter 实例
     */
    public SseEmitter createEmitter(String managerId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.computeIfAbsent(managerId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(managerId, emitter));
        emitter.onTimeout(() -> removeEmitter(managerId, emitter));
        emitter.onError(e -> removeEmitter(managerId, emitter));

        // 发送重连间隔指令
        try {
            emitter.send(SseEmitter.event().reconnectTime(3000));
        } catch (IOException e) {
            log.warn("发送 SSE reconnectTime 失败: managerId={}", managerId);
        }

        log.info("SSE 连接建立: managerId={}, 当前该管理员连接数={}",
                managerId, emitters.getOrDefault(managerId, new CopyOnWriteArrayList<>()).size());

        return emitter;
    }

    /**
     * 向指定管理员的所有连接发送事件
     *
     * @param managerId 管理员 ID
     * @param eventName 事件名称 (如 notification:new, notification:count)
     * @param data      事件数据 (将被 JSON 序列化)
     */
    public void sendToManager(String managerId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> managerEmitters = emitters.get(managerId);
        if (managerEmitters == null || managerEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : managerEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                log.debug("SSE 发送失败, 移除 emitter: managerId={}, event={}", managerId, eventName);
                removeEmitter(managerId, emitter);
            }
        }
    }

    /**
     * 向所有已连接管理员广播事件
     *
     * @param eventName 事件名称
     * @param data      事件数据
     */
    public void broadcast(String eventName, Object data) {
        for (String managerId : emitters.keySet()) {
            sendToManager(managerId, eventName, data);
        }
    }

    /**
     * 检查管理员是否有活跃的 SSE 连接
     */
    public boolean isConnected(String managerId) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(managerId);
        return list != null && !list.isEmpty();
    }

    /**
     * 获取当前已连接管理员数量
     */
    public int getConnectedManagerCount() {
        return emitters.size();
    }

    private void removeEmitter(String managerId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(managerId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(managerId);
            }
        }
    }

    private void sendHeartbeats() {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    removeEmitter(entry.getKey(), emitter);
                }
            }
        }
    }
}
