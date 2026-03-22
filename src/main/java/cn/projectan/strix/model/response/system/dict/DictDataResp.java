package cn.projectan.strix.model.response.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/28 23:27
 */
@Schema(description = "字典数据详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataResp {

    @Schema(description = "字典数据ID")
    private String id;

    @Schema(description = "字典键")
    private String key;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "字典标签")
    private String label;

    @Schema(description = "排序值")
    private Short sort;

    @Schema(description = "样式")
    private String style;

    @Schema(description = "状态")
    private Short status;

    @Schema(description = "备注")
    private String remark;

}
