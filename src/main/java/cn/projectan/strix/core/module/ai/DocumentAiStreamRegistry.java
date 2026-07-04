package cn.projectan.strix.core.module.ai;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档 AI 分析流式注册表
 * <p>
 * 与 {@link AiStreamRegistry}（对话）同源的「后台生成 + 多订阅 + 快照续播 + 终态广播」范式，
 * 但文档分析的事件模型更复杂：一个任务含多个并行批次（batch）与可选的合并（merge）阶段，
 * 事件类型有 {@code stage / batch_chunk / batch_done / batch_error / merge_chunk / done / error}。
 * <p>
 * 核心能力：
 * <ul>
 *   <li>分析过程在后台虚拟线程独立跑完并落库，SSE 连接仅作观众；断开（刷新/切走）不影响分析。</li>
 *   <li>新订阅者 attach 时在锁内回放一帧 {@code snapshot}（当前阶段 + 全部批次已生成内容与状态 +
 *       合并已生成内容），随后继续接收增量，保证不漏帧不重帧。</li>
 *   <li>终态（done/error）广播后置位 finished，后续 attach 直接落空，客户端走结果兜底接口。</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-07-04
 */
@Slf4j
@Component
public class DocumentAiStreamRegistry {

    /**
     * 进行中的分析：taskId → ActiveAnalysis。一个任务至多一条。
     */
    private final ConcurrentHashMap<String, ActiveAnalysis> analyses = new ConcurrentHashMap<>();

    /**
     * 开始一条新的分析。若该任务已有进行中的分析，返回 {@code null}。
     *
     * @param taskId       任务 ID
     * @param totalBatches 总批次数
     * @return 新建的 ActiveAnalysis；已存在则返回 null
     */
    public ActiveAnalysis start(String taskId, int totalBatches) {
        ActiveAnalysis analysis = new ActiveAnalysis(taskId, totalBatches);
        ActiveAnalysis existing = analyses.putIfAbsent(taskId, analysis);
        return existing != null ? null : analysis;
    }

    /**
     * 获取任务进行中的分析，不存在则返回 null。
     */
    public ActiveAnalysis get(String taskId) {
        return analyses.get(taskId);
    }

    /**
     * 移除任务的分析记录（完成 / 出错后调用）。
     */
    public void remove(String taskId) {
        analyses.remove(taskId);
    }

    /**
     * 一条进行中的文档分析。持有全量状态缓冲与当前所有订阅的 SSE 连接。
     */
    public static class ActiveAnalysis {

        private final String taskId;
        @Getter
        private final int totalBatches;

        /**
         * 当前阶段：CONVERTING / ANALYZING / MERGING。
         */
        private String stage = "";

        /**
         * 阶段附加数据（如 batches 列表、totalPages）。随最近一次 setStage 更新，用于快照回放。
         */
        private Map<String, Object> stageData = new LinkedHashMap<>();

        /**
         * 各批次已生成内容缓冲（batchIndex → 内容全量）。
         */
        private final Map<Integer, StringBuilder> batchBuffers = new ConcurrentHashMap<>();

        /**
         * 各批次状态（batchIndex → pending/streaming/done/error）。
         */
        private final Map<Integer, String> batchStatus = new ConcurrentHashMap<>();

        /**
         * 各批次错误信息（batchIndex → message）。
         */
        private final Map<Integer, String> batchErrors = new ConcurrentHashMap<>();

        /**
         * 合并结果已生成内容缓冲（全量）。
         */
        private final StringBuilder mergeBuffer = new StringBuilder();

        /**
         * 当前订阅本次分析的 SSE 连接（支持多标签页）。
         */
        private final List<SseEmitter> subscribers = new ArrayList<>();

        /**
         * 终态标记（完成 / 出错后置位）。
         */
        @Getter
        private volatile boolean finished = false;

        private ActiveAnalysis(String taskId, int totalBatches) {
            this.taskId = taskId;
            this.totalBatches = totalBatches;
        }

        // ============================================================
        //  增量更新 + 广播（均与 subscribe 串行化）
        // ============================================================

        /**
         * 更新当前阶段并广播。
         */
        public synchronized void setStage(String stage, Map<String, Object> data) {
            this.stage = stage;
            this.stageData = data != null ? new LinkedHashMap<>(data) : new LinkedHashMap<>();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("stage", stage);
            payload.putAll(this.stageData);
            broadcast("stage", payload);
        }

        /**
         * 追加某批次的正文增量并广播。
         */
        public synchronized void appendBatchChunk(int batchIndex, String delta) {
            batchBuffers.computeIfAbsent(batchIndex, k -> new StringBuilder()).append(delta);
            batchStatus.put(batchIndex, "streaming");
            broadcast("batch_chunk", Map.of("batchIndex", batchIndex, "content", delta));
        }

        /**
         * 标记某批次完成并广播。
         */
        public synchronized void markBatchDone(int batchIndex) {
            batchStatus.put(batchIndex, "done");
            broadcast("batch_done", Map.of("batchIndex", batchIndex));
        }

        /**
         * 标记某批次出错并广播。
         */
        public synchronized void markBatchError(int batchIndex, String message) {
            batchStatus.put(batchIndex, "error");
            batchErrors.put(batchIndex, message != null ? message : "分析失败");
            broadcast("batch_error", Map.of("batchIndex", batchIndex,
                    "message", message != null ? message : "分析失败"));
        }

        /**
         * 追加合并结果增量并广播。
         */
        public synchronized void appendMergeChunk(String delta) {
            mergeBuffer.append(delta);
            broadcast("merge_chunk", Map.of("content", delta));
        }

        /**
         * 当前某批次全量内容（用于落库）。
         */
        public synchronized String batchContent(int batchIndex) {
            StringBuilder sb = batchBuffers.get(batchIndex);
            return sb != null ? sb.toString() : "";
        }

        /**
         * 当前合并结果全量（用于落库）。
         */
        public synchronized String mergeContent() {
            return mergeBuffer.toString();
        }

        // ============================================================
        //  订阅 / 续播
        // ============================================================

        /**
         * 发起者挂接：直接加入订阅列表（不回放快照，因为发起时缓冲为空）。
         */
        public synchronized void addOriginator(SseEmitter emitter) {
            subscribers.add(emitter);
        }

        /**
         * 新订阅者挂接（续播）：在锁内先回放全量快照（snapshot 事件），再加入订阅列表。
         * <p>快照含当前阶段、全部批次的已生成内容与状态、合并已生成内容，使刷新后 UI 可完整重建。
         *
         * @return true=已挂接（分析仍在进行）；false=已结束，未挂接（客户端应走结果兜底接口）
         */
        public synchronized boolean subscribe(SseEmitter emitter) {
            if (finished) {
                return false;
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("stage", stage);
            snapshot.put("stageData", stageData);
            snapshot.put("totalBatches", totalBatches);
            List<Map<String, Object>> batchList = new ArrayList<>();
            for (int i = 0; i < totalBatches; i++) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("batchIndex", i);
                StringBuilder buf = batchBuffers.get(i);
                b.put("content", buf != null ? buf.toString() : "");
                b.put("status", batchStatus.getOrDefault(i, "pending"));
                if (batchErrors.containsKey(i)) {
                    b.put("errorMessage", batchErrors.get(i));
                }
                batchList.add(b);
            }
            snapshot.put("batches", batchList);
            snapshot.put("mergeContent", mergeBuffer.toString());
            try {
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
            } catch (Exception e) {
                log.debug("doc-ai attach 发送 snapshot 失败, 放弃挂接: taskId={}", taskId);
                return false;
            }
            subscribers.add(emitter);
            return true;
        }

        /**
         * 主动移除一个订阅者（客户端断开时调用）。
         */
        public synchronized void unsubscribe(SseEmitter emitter) {
            subscribers.remove(emitter);
        }

        /**
         * 终态广播：向所有订阅者发送 done/error 并 complete，随后清空订阅并置终态。
         */
        public synchronized void finish(String eventName, Object data) {
            finished = true;
            List<SseEmitter> snapshot = new ArrayList<>(subscribers);
            subscribers.clear();
            for (SseEmitter emitter : snapshot) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("doc-ai 分析终态下发失败: taskId={}, event={}", taskId, eventName);
                }
            }
        }

        /**
         * 向所有订阅者广播事件；发送失败的订阅者就地移除（观众离场），不影响分析。
         */
        private void broadcast(String eventName, Object data) {
            subscribers.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false;
                } catch (Exception e) {
                    log.debug("doc-ai 增量下发失败, 移除订阅者: taskId={}", taskId);
                    return true;
                }
            });
        }
    }
}
