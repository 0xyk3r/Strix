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
 * @since 2021-08-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_wechat_user")
public class WechatUser extends BaseModel {

    @Serial
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
