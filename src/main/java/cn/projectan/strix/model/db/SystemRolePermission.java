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
 * @since 2021-05-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_role_permission")
public class SystemRolePermission extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 系统角色id
     */
    private String systemRoleId;

    /**
     * 系统权限id
     */
    private String systemPermissionId;


}
