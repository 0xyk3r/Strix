package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/30 12:07
 */
@Schema(description = "字典数据列表请求")
@Data
public class DictDataListReq extends BasePageReq<DictData> {

    @Schema(description = "搜索关键词", example = "正常")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "字典数据状态", example = "1")
    private Short status;

}
