package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/30 11:06
 */
@Schema(description = "字典列表请求")
@Data
public class DictListReq extends BasePageReq<Dict> {

    @Schema(description = "搜索关键词", example = "系统")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "字典状态", example = "1")
    private Short status;

    @Schema(description = "是否系统内置", example = "1")
    private Short provided;

}
