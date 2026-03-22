package cn.projectan.strix.model.response.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/6/11 17:45
 */
@Schema(description = "分页基础响应")
@Data
@NoArgsConstructor
public class BasePageResp {

    @Schema(description = "总记录数")
    private Long total;

}
