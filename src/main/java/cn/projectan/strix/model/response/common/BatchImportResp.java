package cn.projectan.strix.model.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入结果
 *
 * @author ProjectAn
 */
@Schema(description = "批量导入结果")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchImportResp {

    @Schema(description = "总行数")
    private int total;

    @Schema(description = "成功数")
    private int successCount;

    @Schema(description = "失败数")
    private int failedCount;

    @Schema(description = "跳过数（重复数据）")
    private int skippedCount;

    @Schema(description = "错误详情列表")
    private List<ImportError> errors;

    @Schema(description = "单行导入错误")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportError {

        @Schema(description = "行号（从 0 开始）")
        private int row;

        @Schema(description = "出错字段")
        private String field;

        @Schema(description = "错误信息")
        private String message;

    }

}
