package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix OSS 配置
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oss_config")
public class OssConfig extends BaseModel<OssConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 配置 Key
     */
    @TableField("`key`")
    @UniqueField("配置 Key ")
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
