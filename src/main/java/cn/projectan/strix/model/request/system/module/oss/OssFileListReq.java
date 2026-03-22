package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/26 19:14
 */
@Schema(description = "OSS 文件列表请求")
@Data
public class OssFileListReq extends BasePageReq<OssFile> {

    @Schema(description = "搜索关键词", example = "avatar")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    private String configKey;

    @Schema(description = "文件分组 Key", example = "avatar")
    private String groupKey;

}
