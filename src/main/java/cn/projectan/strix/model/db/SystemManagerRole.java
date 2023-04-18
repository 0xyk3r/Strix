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
 * @since 2021-05-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_manager_role")
public class SystemManagerRole extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 系统管理员ID
     */
    private String systemManagerId;

    /**
     * 系统管理员角色ID
     */
    private String systemManagerRoleId;


}
