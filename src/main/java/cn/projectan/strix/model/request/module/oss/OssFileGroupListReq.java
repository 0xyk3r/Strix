package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/26 19:14
 */
@Data
public class OssFileGroupListReq extends BasePageReq<OssFileGroup> {

    private String keyword;

    private String configKey;

}
