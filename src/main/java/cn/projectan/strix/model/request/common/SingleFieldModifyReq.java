package cn.projectan.strix.model.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单一属性修改请求参数
 *
 * @author ProjectAn
 * @since 2021/6/16 15:18
 */
@Schema(description = "单字段修改请求")
@Data
public class SingleFieldModifyReq {

    @NotBlank(message = "字段名称不能为空")
    @Size(max = 50, message = "字段名称长度不能超过50")
    @Schema(description = "字段名称", example = "status")
    private String field;

    @Size(max = 500, message = "字段值长度不能超过500")
    @Schema(description = "字段值", example = "1")
    private String value;

}
