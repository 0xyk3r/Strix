package cn.projectan.strix.model.response.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/27 21:30
 */
@Schema(description = "文件分组详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssFileGroupResp {

    @Schema(description = "分组ID")
    private String id;

    @Schema(description = "分组标识")
    private String key;

    @Schema(description = "配置标识")
    private String configKey;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "Bucket 名称")
    private String bucketName;

    @Schema(description = "Bucket 域名")
    private String bucketDomain;

    @Schema(description = "基础目录")
    private String baseDir;

    @Schema(description = "允许的文件扩展名")
    private String allowExtension;

    @Schema(description = "加密类型")
    private Short secretType;

    @Schema(description = "加密等级")
    private Short secretLevel;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
