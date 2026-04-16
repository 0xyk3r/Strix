package cn.projectan.strix.model.request.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典全局搜索请求")
@Data
public class DictGlobalSearchReq {

    @Schema(description = "搜索关键词")
    @NotEmpty(message = "搜索关键词不能为空")
    @Size(max = 64, message = "搜索关键词长度不能超过 64")
    private String keyword;

}
