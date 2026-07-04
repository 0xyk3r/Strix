package cn.projectan.strix.model.response.system.tool.document;

import cn.projectan.strix.core.module.ai.AiJson;
import cn.projectan.strix.model.db.system.AiDocumentTask;
import lombok.Data;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档 AI 分析结果响应（兜底接口）
 * <p>
 * 分析已结束、SSE 无进行中生成时用于拉取落库的最终结果：状态、各批次结果、合并结果、错误信息。
 * 前端刷新页面后可据此完整重建结果视图，不再仅依赖丢失即无的 SSE 流。
 *
 * @author ProjectAn
 * @since 2026-07-04
 */
@Data
public class DocumentAiResultResp {

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 任务状态：PROCESSING / DONE / FAILED
     */
    private String status;

    /**
     * 输入类型：DOC / TEXT
     */
    private String inputType;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 分析提示词
     */
    private String prompt;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 总批次数
     */
    private Integer totalBatches;

    /**
     * 是否启用合并
     */
    private boolean mergeEnabled;

    /**
     * 各批次页码范围描述
     */
    private List<String> batchDescriptions;

    /**
     * 各批次结果（按批次索引升序）
     */
    private List<BatchResult> batchResults;

    /**
     * 合并结果
     */
    private String mergeResult;

    /**
     * 错误信息（FAILED 时）
     */
    private String errorMessage;

    @Data
    public static class BatchResult {
        private int batchIndex;
        private String content;

        public BatchResult(int batchIndex, String content) {
            this.batchIndex = batchIndex;
            this.content = content;
        }
    }

    public static DocumentAiResultResp from(AiDocumentTask task) {
        DocumentAiResultResp resp = new DocumentAiResultResp();
        resp.setTaskId(task.getId());
        resp.setStatus(task.getStatus());
        resp.setInputType(task.getInputType());
        resp.setFileName(task.getFileName());
        resp.setPrompt(task.getPrompt());
        resp.setTotalPages(task.getTotalPages());
        resp.setTotalBatches(task.getTotalBatches());
        resp.setMergeEnabled(task.getMergeEnabled() != null && task.getMergeEnabled() == 1);
        resp.setBatchDescriptions(parseList(task.getBatchDescriptions()));
        resp.setBatchResults(parseBatchResults(task.getBatchResults(),
                task.getTotalBatches() != null ? task.getTotalBatches() : 0));
        resp.setMergeResult(task.getMergeResult());
        resp.setErrorMessage(task.getErrorMessage());
        return resp;
    }

    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return AiJson.mapper().readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析批次结果 JSON（{"0":"...","1":"..."}）为按索引升序的列表。
     */
    private static List<BatchResult> parseBatchResults(String json, int totalBatches) {
        Map<Integer, String> map = new HashMap<>();
        if (json != null && !json.isBlank()) {
            try {
                Map<String, String> raw = AiJson.mapper().readValue(json, new TypeReference<>() {
                });
                raw.forEach((k, v) -> {
                    try {
                        map.put(Integer.parseInt(k), v);
                    } catch (NumberFormatException ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
        List<BatchResult> list = new ArrayList<>();
        int count = Math.max(totalBatches, map.keySet().stream().mapToInt(i -> i + 1).max().orElse(0));
        for (int i = 0; i < count; i++) {
            list.add(new BatchResult(i, map.getOrDefault(i, "")));
        }
        return list;
    }
}
