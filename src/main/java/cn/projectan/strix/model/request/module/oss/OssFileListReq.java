package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/26 19:14
 */
@Data
public class OssFileListReq extends BasePageReq<OssFile> {

    private String keyword;

    private String configKey;

    private String groupKey;

}
