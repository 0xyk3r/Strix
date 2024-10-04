package cn.projectan.strix.model.response.system.dict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/30 11:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictResp {

    private String id;

    private String key;

    private String name;

    private Integer dataType;

    private Integer status;

    private String remark;

    private Integer version;

    private Integer provided;

    private List<DictDataListResp.DictDataItem> dictDataList;

}
