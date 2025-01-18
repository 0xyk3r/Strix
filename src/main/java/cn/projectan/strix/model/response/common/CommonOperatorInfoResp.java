package cn.projectan.strix.model.response.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * @author ProjectAn
 * @since 2025-01-18 10:41:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonOperatorInfoResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String operatorId;

    private Short operatorType;

    private String operatorName;

    private Object operatorInfo;

}
