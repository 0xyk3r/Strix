package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/5/28 23:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonDictResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private String id;

    private String key;

    private Integer dataType;

    private Integer version;

    private List<DictDataListResp.DictDataItem> dictDataList;

}
