package cn.projectan.strix.model.response.system.tool.document;

import lombok.Data;

/**
 * 文档转换任务提交响应
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@Data
public class DocumentConvertSubmitResp {

    /**
     * 任务 ID
     */
    private String taskId;

    public DocumentConvertSubmitResp(String taskId) {
        this.taskId = taskId;
    }

}
