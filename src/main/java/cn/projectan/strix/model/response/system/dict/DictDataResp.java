package cn.projectan.strix.model.response.system.dict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/28 23:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataResp {

    private String id;

    private String key;

    private String value;

    private String label;

    private Integer sort;

    private String style;

    private Integer status;

    private String remark;

}
