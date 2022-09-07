package cn.projectan.strix.model.db;

import com.baomidou.mybatisplus.annotation.TableName;
import cn.projectan.strix.model.db.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@TableName("tab_aliyun_sms")
public class AliyunSms extends BaseModel {

    private static final long serialVersionUID = 1L;

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
