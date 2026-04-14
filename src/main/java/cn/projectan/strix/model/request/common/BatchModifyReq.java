package cn.projectan.strix.model.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "批量字段修改请求")
@Data
public class BatchModifyReq {

    @NotEmpty(message = "{validation.required:field.ids}")
    @Size(max = 100, message = "{validation.batch.limit}")
    @Schema(description = "待修改的 ID 列表", example = "[\"1\", \"2\", \"3\"]")
    private List<String> ids;

    @NotBlank(message = "{validation.required:field.fieldName}")
    @Size(max = 50, message = "{validation.length:field.fieldName}")
    @Schema(description = "字段名称", example = "status")
    private String field;

    @Size(max = 500, message = "{validation.length:field.fieldValue}")
    @Schema(description = "字段值", example = "1")
    private String value;

}
