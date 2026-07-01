package cn.projectan.strix.core.module.ai;

import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 流式生成注册表
 * <p>
 * 将「AI 生成过程」与「客户端 SSE 连接」解耦：生成始终在后台虚拟线程中独立跑完，
 * 客户端 SSE 连接仅作为「观众」订阅增量。客户端断开（刷新页面 / 切换会话 / 关闭标签页）
 * 只是观众离场，不影响后台生成；客户端重新进入时可通过 attach 重新挂接到进行中的生成，
 * 先回放已生成的全量快照，再继续接收后续增量。
 * <p>
 * 并发模型：
 * <ul>
 *   <li>每个会话至多一条进行中的生成（{@link ActiveGeneration}），以 sessionId 为键。</li>
 *   <li>单条生成内部对「追加缓冲 + 广播」与「新订阅者 attach 回放」使用同一把锁串行化，
 *       保证 attach 时拿到的快照与其后收到的增量既不遗漏也不重复。</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-07-01
 */
@Slf4j
@Component
public class AiStreamRegistry {

    /**
     * 进行中的生成：sessionId → ActiveGeneration。一个会话至多一条。
     */
    private final ConcurrentHashMap<String, ActiveGeneration> generations = new ConcurrentHashMap<>();

    /**
     * 开始一条新的生成。若该会话已有进行中的生成，返回 {@code null}（调用方应据此拒绝并发生成）。
     *
     * @param sessionId      会话 ID
     * @param assistantMsgId assistant 占位消息 ID
     * @param userMsgId      user 消息 ID（重新生成时为 null）
     * @param firstEmitter   发起本次请求的 SSE 连接（作为第一个订阅者，可为 null）
     * @return 新建的 ActiveGeneration；若已存在进行中的生成则返回 null
     */
    public ActiveGeneration start(String sessionId, String assistantMsgId, String userMsgId, SseEmitter firstEmitter) {
        ActiveGeneration generation = new ActiveGeneration(sessionId, assistantMsgId, userMsgId);
        ActiveGeneration existing = generations.putIfAbsent(sessionId, generation);
        if (existing != null) {
            return null;
        }
        if (firstEmitter != null) {
            // 发起者：缓冲为空，无需回放快照，直接加入订阅列表
            generation.addOriginator(firstEmitter);
        }
        return generation;
    }

    /**
     * 获取会话进行中的生成，不存在则返回 null。
     */
    public ActiveGeneration get(String sessionId) {
        return generations.get(sessionId);
    }

    /**
     * 移除会话的生成记录（生成完成 / 出错 / 被停止后调用）。
     */
    public void remove(String sessionId) {
        generations.remove(sessionId);
    }

    /**
     * 一条进行中的 AI 生成。持有已生成内容缓冲与当前所有订阅的 SSE 连接。
     */
    public static class ActiveGeneration {

        private final String sessionId;
        @Getter
        private final String assistantMsgId;
        @Getter
        private final String userMsgId;

        /**
         * 已生成的正文缓冲（全量）。
         */
        private final StringBuilder contentBuffer = new StringBuilder();

        /**
         * 已生成的思考内容缓冲（全量）。
         */
        private final StringBuilder thinkingBuffer = new StringBuilder();

        /**
         * 当前订阅本次生成的 SSE 连接（支持多标签页）。
         */
        private final List<SseEmitter> subscribers = new ArrayList<>();

        /**
         * 用户主动停止标记。置位后，上游读循环会在下一个 chunk 处中止并落库已生成部分。
         */
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);

        /**
         * 终态标记（完成 / 出错 / 停止后置位），避免完成后仍有 attach 挂接到已结束的生成。
         */
        @Getter
        private volatile boolean finished = false;

        private ActiveGeneration(String sessionId, String assistantMsgId, String userMsgId) {
            this.sessionId = sessionId;
            this.assistantMsgId = assistantMsgId;
            this.userMsgId = userMsgId;
        }

        public boolean isStopRequested() {
            return stopRequested.get();
        }

        /**
         * 请求停止本次生成（用户主动点击停止）。
         */
        public void requestStop() {
            stopRequested.set(true);
        }

        /**
         * 追加正文增量并广播给所有订阅者。与 {@link #subscribe} 串行化。
         */
        public synchronized void appendContent(String delta) {
            contentBuffer.append(delta);
            broadcast(AiSseEvent.CONTENT, Map.of("content", delta));
        }

        /**
         * 追加思考增量并广播给所有订阅者。与 {@link #subscribe} 串行化。
         */
        public synchronized void appendThinking(String delta) {
            thinkingBuffer.append(delta);
            broadcast(AiSseEvent.THINKING, Map.of("content", delta));
        }

        /**
         * 发起者挂接：缓冲为空，直接加入订阅列表（不下发 snapshot）。仅供 {@link #start} 内部调用。
         */
        synchronized void addOriginator(SseEmitter emitter) {
            subscribers.add(emitter);
        }

        /**
         * 新订阅者挂接：在锁内先回放已生成的全量快照（snapshot 事件），再加入订阅列表。
         * 由于与 append 共用锁，snapshot 与其后广播的增量之间不会漏帧或重帧。
         *
         * @param emitter 新的 SSE 连接
         * @return true=已挂接（生成仍在进行）；false=生成已结束，未挂接（调用方应让客户端走历史消息兜底）
         */
        public synchronized boolean subscribe(SseEmitter emitter) {
            if (finished) {
                return false;
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("content", contentBuffer.toString());
            snapshot.put("thinkingContent", thinkingBuffer.toString());
            snapshot.put("assistantMessageId", assistantMsgId);
            if (userMsgId != null) {
                snapshot.put("userMessageId", userMsgId);
            }
            try {
                emitter.send(SseEmitter.event().name(AiSseEvent.SNAPSHOT).data(snapshot));
            } catch (Exception e) {
                // 快照都发不出去，说明该连接已失效，不加入订阅
                log.debug("AI attach 发送 snapshot 失败, 放弃挂接: sessionId={}", sessionId);
                return false;
            }
            subscribers.add(emitter);
            return true;
        }

        /**
         * 主动移除一个订阅者（客户端断开时调用）。不影响生成与其它订阅者。
         */
        public synchronized void unsubscribe(SseEmitter emitter) {
            subscribers.remove(emitter);
        }

        /**
         * 终态广播：向所有订阅者发送 done/error 事件并 complete，随后清空订阅并置终态。
         * 由调用方在生成结束时（成功 / 出错 / 停止落库后）调用。
         *
         * @param eventName 事件名（done / error）
         * @param data      事件数据
         */
        public synchronized void finish(String eventName, Object data) {
            finished = true;
            // 先快照并清空：emitter.complete() 会同步触发订阅方的 onCompletion 回调（进而调用 unsubscribe），
            // 若在遍历原列表时移除元素会抛 ConcurrentModificationException。
            List<SseEmitter> snapshot = new ArrayList<>(subscribers);
            subscribers.clear();
            for (SseEmitter emitter : snapshot) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("AI 生成终态下发失败: sessionId={}, event={}", sessionId, eventName);
                }
            }
        }

        /**
         * 当前正文全量（用于落库）。
         */
        public synchronized String currentContent() {
            return contentBuffer.toString();
        }

        /**
         * 当前思考全量（用于落库）。
         */
        public synchronized String currentThinking() {
            return thinkingBuffer.toString();
        }

        /**
         * 向所有订阅者广播事件；发送失败的订阅者就地移除（观众离场），不影响生成。
         */
        private void broadcast(String eventName, Object data) {
            subscribers.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false;
                } catch (Exception e) {
                    log.debug("AI 增量下发失败, 移除订阅者: sessionId={}", sessionId);
                    return true;
                }
            });
        }
    }
}
