package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/28 23:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用 - 字典 - 获取字典数据响应")
public class CommonDictResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    @Schema(description = "字典 ID")
    private String id;

    @Schema(description = "字典 Key")
    private String key;

    @Schema(description = "字典名称")
    private Short dataType;

    @Schema(description = "字典版本")
    private Integer version;

    @Schema(description = "字典数据列表")
    private List<DictDataListResp.DictDataItem> dictDataList;

}
