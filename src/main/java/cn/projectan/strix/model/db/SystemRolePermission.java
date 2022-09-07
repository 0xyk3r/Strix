package cn.projectan.strix.model.db;

import com.baomidou.mybatisplus.annotation.TableName;
import cn.projectan.strix.model.db.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tab_system_role_permission")
public class SystemRolePermission extends BaseModel {

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
