package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/26 18:24
 */
@Schema(description = "OSS Bucket 列表请求")
@Data
public class OssBucketListReq extends BasePageReq<OssBucket> {

    @Schema(description = "搜索关键词", example = "images")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    private String configKey;

}
