package cn.projectan.strix.model.request.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典克隆请求")
@Data
public class DictCloneReq {

    @Schema(description = "新字典 Key")
    @NotEmpty(message = "新字典 Key 不能为空")
    @Size(min = 2, max = 64, message = "新字典 Key 长度应在 2-64 之间")
    private String newKey;

    @Schema(description = "新字典名称")
    @NotEmpty(message = "新字典名称不能为空")
    @Size(min = 2, max = 32, message = "新字典名称长度应在 2-32 之间")
    private String newName;

}
