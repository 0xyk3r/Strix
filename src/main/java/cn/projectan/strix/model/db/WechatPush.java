package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
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
 * @since 2022-03-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_wechat_push")
public class WechatPush extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 微信配置id
     */
    private String configId;

    /**
     * appid
     */
    private String appId;

    /**
     * 系统用户id
     */
    private String systemUserId;

    /**
     * 微信用户id
     */
    private String wechatUserId;

    /**
     * 用户openid
     */
    private String openId;

    /**
     * 模板内容
     */
    private String templateBody;

    /**
     * 推送状态 1待推送 2推送成功 3推送失败
     */
    private Integer pushStatus;

    /**
     * 微信服务器返回结果
     */
    private String resultBody;


}
