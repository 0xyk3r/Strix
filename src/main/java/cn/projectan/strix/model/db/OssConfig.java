package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * <p>
 * 阿里云OSS配置
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_config")
public class OssConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 配置 Key
     */
    @TableField("`key`")
    @UniqueDetection("配置 Key ")
    private String key;

    /**
     * OSS 配置名称
     */
    @TableField("`name`")
    private String name;

    /**
     * OSS 服务平台
     */
    private Integer platform;

    /**
     * 公网连接域名
     */
    private String publicEndpoint;

    /**
     * 私网连接域名
     */
    private String privateEndpoint;

    /**
     * 授权令牌ID
     */
    private String accessKey;

    /**
     * 授权令牌秘钥
     */
    private String accessSecret;

    /**
     * 备注
     */
    private String remark;

}
