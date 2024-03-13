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
 * @author ProjectAn
 * @since 2021-08-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_wechat_config")
public class WechatConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 微信公众号名称
     */
    private String name;

    /**
     * 微信公众号开发者ID(AppID)
     */
    private String appId;

    /**
     * 微信公众号开发者密码(AppSecret)
     */
    private String appSecret;

    /**
     * 微信公众号令牌(Token)
     */
    private String token;

    /**
     * 微信公众号首页地址
     */
    private String webIndexUrl;

    /**
     * 微信公众号授权回调地址
     */
    private String authUrl;


}
