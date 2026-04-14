package cn.projectan.strix.model.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "批量删除请求")
@Data
public class BatchRemoveReq {

    @NotEmpty(message = "{validation.required:field.ids}")
    @Size(max = 100, message = "{validation.batch.limit}")
    @Schema(description = "待删除的 ID 列表", example = "[\"1\", \"2\", \"3\"]")
    private List<String> ids;

}
