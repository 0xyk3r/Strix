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
 * @since 2021-08-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tab_wechat_user")
public class WechatUser extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 微信配置id
     */
    private String configId;

    /**
     * 微信app_id
     */
    private String appId;

    /**
     * 微信用户open_id
     */
    private String openId;


}
