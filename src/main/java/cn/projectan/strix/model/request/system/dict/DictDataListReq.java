package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/30 12:07
 */
@Data
public class DictDataListReq extends BasePageReq<DictData> {

    @Size(max = 64)
    private String keyword;

    private Short status;

}
