package cn.projectan.strix.model.request.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典数据排序请求")
@Data
public class DictSortReq {

    @Schema(description = "按顺序排列的字典数据 ID 列表")
    @NotEmpty(message = "排序列表不能为空")
    private List<String> sortedIds;

}
