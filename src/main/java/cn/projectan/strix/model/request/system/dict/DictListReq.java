package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/30 11:06
 */
@Data
public class DictListReq extends BasePageReq<Dict> {

    private String keyword;

    private Integer status;

    private Integer provided;

}
