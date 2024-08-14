package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_manager")
public class SystemManager extends BaseModel {

    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * 显示昵称
     */
    @UniqueDetection("昵称")
    private String nickname;

    /**
     * 登录账号
     */
    @UniqueDetection("登录账号")
    private String loginName;

    /**
     * 登录密码
     */
    @JsonIgnore
    private String loginPassword;

    /**
     * 管理人员状态 1正常 2禁止登录
     */
    private Integer status;

    /**
     * 管理人员类型 1超级账户 2普通账户
     */
    private Integer type;

    /**
     * 平台账户拥有的地区权限
     */
    private String regionId;

    /**
     * 是否系统内置用户
     */
    private Byte builtin;

}
