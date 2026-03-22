package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/26 19:14
 */
@Schema(description = "文件分组列表请求")
@Data
public class OssFileGroupListReq extends BasePageReq<OssFileGroup> {

    @Schema(description = "搜索关键词", example = "头像")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    private String configKey;

}
