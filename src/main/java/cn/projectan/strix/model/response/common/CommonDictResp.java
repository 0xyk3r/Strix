package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/28 23:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonDictResp {

    private String id;

    private String key;

    private Integer version;

    private List<DictDataListResp.DictDataItem> dictDataList;

}
