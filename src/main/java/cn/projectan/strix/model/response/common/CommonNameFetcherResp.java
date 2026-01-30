package cn.projectan.strix.model.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/30 20:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用 - 数据 ID 映射器响应")
public class CommonNameFetcherResp {

    @Schema(description = "名称")
    private String name;

}
