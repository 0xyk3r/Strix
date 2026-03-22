package cn.projectan.strix.model.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "字段名称", example = "status")
    private String field;

    @Schema(description = "字段值", example = "1")
    private String value;

}
