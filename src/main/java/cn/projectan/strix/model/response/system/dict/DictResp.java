package cn.projectan.strix.model.response.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/30 11:03
 */
@Schema(description = "字典详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictResp {

    @Schema(description = "字典ID")
    private String id;

    @Schema(description = "字典键")
    private String key;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "数据类型")
    private Short dataType;

    @Schema(description = "状态")
    private Short status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否系统内置")
    private Short provided;

    @Schema(description = "字典数据列表")
    private List<DictDataListResp.DictDataItem> dictDataList;

}
