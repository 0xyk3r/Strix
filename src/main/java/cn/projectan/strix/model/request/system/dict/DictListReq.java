package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/30 11:06
 */
@Data
public class DictListReq extends BasePageReq<Dict> {

    @Size(max = 64)
    private String keyword;

    private Short status;

    private Short provided;

}
