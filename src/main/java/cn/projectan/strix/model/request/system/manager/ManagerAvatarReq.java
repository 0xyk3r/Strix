package cn.projectan.strix.model.request.system.manager;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量查询管理员头像配置请求
 *
 * @author ProjectAn
 */
@Schema(description = "批量查询管理员头像配置请求")
@Data
public class ManagerAvatarReq {

    @NotEmpty(message = "{validation.required:field.ids}")
    @Size(max = 200, message = "{validation.batch.limit}")
    @Schema(description = "管理员 ID 列表", example = "[\"1\", \"2\", \"3\"]")
    private List<String> ids;

}
