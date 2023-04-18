package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_sms_log")
public class SystemSmsLog extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 短信发送平台 1阿里云
     */
    private Integer smsPlatform;

    /**
     * 使用的短信配置id
     */
    private String smsConfigId;

    /**
     * 短信发往号码
     */
    private String phoneNumber;

    /**
     * 请求短信用户ip地址
     */
    private String senderIpAddress;

    /**
     * 短信签名
     */
    private String smsSignName;

    /**
     * 短信模板
     */
    private String smsTemplateCode;

    /**
     * 短信参数
     */
    private String smsTemplateParam;

    /**
     * 0待发送 1已发送 2发送失败
     */
    private Integer smsSendStatus;

    /**
     * 短信平台返回结果
     */
    private String smsPlatformResponse;


}
