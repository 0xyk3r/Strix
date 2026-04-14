package cn.projectan.strix.model.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量导入请求
 *
 * @author ProjectAn
 */
@Schema(description = "批量导入请求")
@Data
public class BatchImportReq {

    @NotEmpty(message = "{validation.required:field.importItems}")
    @Size(max = 1000, message = "{validation.batch.import.limit}")
    @Schema(description = "导入数据列表")
    private List<Map<String, Object>> items;

    @Schema(description = "重复数据处理策略: SKIP / UPSERT", example = "SKIP")
    private String duplicateStrategy;

}
