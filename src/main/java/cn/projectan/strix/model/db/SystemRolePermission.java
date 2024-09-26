package cn.projectan.strix.model.db;

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
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
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
