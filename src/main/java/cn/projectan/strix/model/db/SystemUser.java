package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix 系统用户
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_user")
public class SystemUser extends BaseModel<SystemUser> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     */
    @UniqueField("用户昵称")
    private String nickname;

    /**
     * 1启用 2禁用
     */
    private Integer status;

    /**
     * 用户手机号码
     */
    @UniqueField("用户手机号码")
    private String phoneNumber;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 密码
     */
    private String loginPass;

}
