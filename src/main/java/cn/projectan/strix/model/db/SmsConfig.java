package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import cn.projectan.strix.model.dict.StrixSmsPlatform;
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
 * 短信服务配置
 * </p>
 *
 * @author ProjectAn
 * @since 2023/5/22 11:59
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sms_config")
public class SmsConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * key
     */
    @TableField("`key`")
    @UniqueDetection("配置 Key ")
    private String key;

    /**
     * 短信服务名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 短信服务平台
     *
     * @see StrixSmsPlatform
     */
    @UniqueDetection(value = "服务平台", group = 1)
    private Integer platform;

    /**
     * 短信服务地区ID
     */
    @UniqueDetection(value = "服务地域", group = 1)
    private String regionId;

    /**
     * 授权令牌key
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
