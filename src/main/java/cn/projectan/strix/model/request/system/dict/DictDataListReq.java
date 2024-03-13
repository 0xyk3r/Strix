package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/30 12:07
 */
@Data
public class DictDataListReq extends BasePageReq<DictData> {

    private String keyword;

    private Integer status;

}
