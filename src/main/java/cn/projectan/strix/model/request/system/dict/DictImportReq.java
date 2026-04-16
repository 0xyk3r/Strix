package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.response.system.dict.DictExportData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典导入请求")
@Data
public class DictImportReq {

    @Schema(description = "字典数据列表")
    @NotEmpty(message = "导入数据不能为空")
    private List<DictExportData> dicts;

    @Schema(description = "冲突策略: SKIP / OVERWRITE / RENAME")
    @NotNull(message = "冲突策略不能为空")
    private String conflictStrategy;

}
