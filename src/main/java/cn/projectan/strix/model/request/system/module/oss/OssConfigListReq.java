package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/23 11:57
 */
@Schema(description = "OSS 配置列表请求")
@Data
public class OssConfigListReq extends BasePageReq<OssConfig> {

    @Schema(description = "搜索关键词", example = "阿里云")
    @Size(max = 64)
    private String keyword;

}
