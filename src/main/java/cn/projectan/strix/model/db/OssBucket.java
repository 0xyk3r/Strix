package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-23
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oss_bucket")
public class OssBucket extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 配置 Key
     */
    private String configKey;

    /**
     * OSS Bucket 名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 公网连接域名
     */
    private String publicEndpoint;

    /**
     * 内网连接域名
     */
    private String privateEndpoint;

    /**
     * 地域
     */
    private String region;

    /**
     * 存储类型
     */
    private String storageClass;

    /**
     * 备注
     */
    private String remark;

    public OssBucket(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public OssBucket(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
