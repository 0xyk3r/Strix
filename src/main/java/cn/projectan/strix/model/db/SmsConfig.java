package cn.projectan.strix.model.db;

import cn.projectan.strix.model.constant.StrixSmsPlatform;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * 阿里云短信服务配置
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sms_config")
public class SmsConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 短信服务平台
     *
     * @see StrixSmsPlatform
     */
    private Integer platform;

    /**
     * 短信服务地区ID
     */
    private String regionId;

    /**
     * 授权令牌key
     */
    private String accessKey;

    /**
     * 授权令牌秘钥
     */
    private String accessSecret;


}
