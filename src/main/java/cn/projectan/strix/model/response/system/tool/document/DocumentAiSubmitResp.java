package cn.projectan.strix.model.response.system.tool.document;

import lombok.Data;

import java.util.List;

/**
 * 文档 AI 分析任务提交响应
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@Data
public class DocumentAiSubmitResp {

    /**
     * 任务 ID（用于建立 SSE 连接和获取图片）
     */
    private String taskId;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 总批次数
     */
    private int totalBatches;

    /**
     * 每批页面范围描述列表，如 ["第 1~5 页", "第 6~10 页"]
     */
    private List<String> batchDescriptions;

    /**
     * 每批次对应的页面索引范围（0-based，inclusive），如 [[0,4],[5,9]]
     * 可用于前端构造缩略图 URL
     */
    private List<List<Integer>> batchPageRanges;

    public DocumentAiSubmitResp(String taskId, int totalPages, int totalBatches,
                                List<String> batchDescriptions,
                                List<List<Integer>> batchPageRanges) {
        this.taskId = taskId;
        this.totalPages = totalPages;
        this.totalBatches = totalBatches;
        this.batchDescriptions = batchDescriptions;
        this.batchPageRanges = batchPageRanges;
    }

}

