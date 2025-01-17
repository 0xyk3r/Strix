package cn.projectan.strix.model.response.module.oss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/27 21:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssFileGroupResp {

    private String id;

    private String key;

    private String configKey;

    private String name;

    private String bucketName;

    private String bucketDomain;

    private String baseDir;

    private String allowExtension;

    private Integer secretType;

    private Integer secretLevel;

    private String remark;

    private LocalDateTime createdTime;

}
