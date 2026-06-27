package cn.projectan.strix.model.response.system.manager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 批量查询管理员头像配置响应
 *
 * @author ProjectAn
 */
@Schema(description = "批量查询管理员头像配置响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerAvatarResp {

    @Schema(description = "管理员 ID -> 头像配置 JSON 映射（无配置的为 null）")
    private Map<String, String> avatars;

}
