package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import cn.projectan.strix.model.dict.StrixSmsPlatform;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

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
@TableName("sys_sms_log")
public class SmsLog extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用的短信配置 Key
     */
    private String configKey;

    /**
     * 短信发送平台
     *
     * @see StrixSmsPlatform
     */
    private Integer platform;

    /**
     * 短信发往号码
     */
    private String phoneNumber;

    /**
     * 请求短信用户ip地址
     */
    private String requesterIp;

    /**
     * 短信签名
     */
    private String signName;

    /**
     * 短信模板
     */
    private String templateCode;

    /**
     * 短信参数
     */
    private String templateParam;

    /**
     * 0待发送 1已发送 2发送失败
     */
    private Integer status;

    /**
     * 短信平台返回结果
     */
    private String platformResponse;


}
