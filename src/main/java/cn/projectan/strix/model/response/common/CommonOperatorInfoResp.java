package cn.projectan.strix.model.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * @author ProjectAn
 * @since 2025-01-18 10:41:50
 */
@Schema(description = "操作人信息响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonOperatorInfoResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "操作人类型")
    private Short operatorType;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作人详细信息")
    private Object operatorInfo;

}
