package cn.projectan.strix.model.response.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典导出数据结构")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictExportData {

    @Schema(description = "字典 Key")
    private String key;
    @Schema(description = "字典名称")
    private String name;
    @Schema(description = "数据类型")
    private Short dataType;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "数据项列表")
    private List<ExportDataItem> items;

    @Schema(description = "导出数据项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportDataItem {
        private String value;
        private String label;
        private Short sort;
        private String style;
        private Short status;
        private String remark;
        private String parentValue;
        private Short isDefault;
    }

}
