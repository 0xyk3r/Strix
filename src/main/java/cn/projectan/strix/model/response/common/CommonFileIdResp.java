package cn.projectan.strix.model.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/30 20:14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用 - 文件 ID 响应")
public class CommonFileIdResp {

    @Schema(description = "文件 ID")
    private String fileId;

}
