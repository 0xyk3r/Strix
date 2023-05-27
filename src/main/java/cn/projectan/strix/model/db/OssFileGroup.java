package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
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
 * @author 安炯奕
 * @since 2023-05-22
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oss_file_group")
public class OssFileGroup extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件组 Key
     */
    @TableField("`key`")
    @UniqueDetection("配置 Key ")
    private String key;

    /**
     * OSS 配置 Key
     */
    private String configKey;

    /**
     * 文件组名称
     */
    @TableField("`name`")
    private String name;

    /**
     * OSS Bucket 名称
     */
    private String bucketName;

    /**
     * OSS Bucket 自定义域名
     */
    private String bucketDomain;

    /**
     * OSS Bucket 中的基础文件路径
     */
    private String baseDir;

    /**
     * 允许的文件扩展名 需要加. 使用,连接
     */
    private String allowExtension;

    /**
     * 查看权限类型 1管理端文件 2用户端文件
     */
    private Integer secretType;

    /**
     * 查看权限等级 越大等级越高
     */
    private Integer secretLevel;

    /**
     * 备注
     */
    private String remark;

    public OssFileGroup(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public OssFileGroup(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
