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

    @NotBlank(message = "{validation.required:field.fieldName}")
    @Size(max = 50, message = "{validation.length:field.fieldName}")
    @Schema(description = "字段名称", example = "status")
    private String field;

    @Size(max = 500, message = "{validation.length:field.fieldValue}")
    @Schema(description = "字段值", example = "1")
    private String value;

}
