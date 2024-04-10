package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 * Strix OAuth 第三方用户信息
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oauth_user")
public class OauthUser extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OAuth 配置ID
     */
    private String configId;

    /**
     * 第三方平台APPID
     */
    private String appId;

    /**
     * 平台用户OPENID
     */
    private String openId;

    /**
     * 平台用户UNIONID
     */
    private String unionId;

    /**
     * 所属平台
     */
    private Integer platform;

    public OauthUser(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public OauthUser(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
