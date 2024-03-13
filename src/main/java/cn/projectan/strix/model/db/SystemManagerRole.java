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
@TableName("sys_system_manager_role")
public class SystemManagerRole extends BaseModel {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * 系统管理员ID
     */
    private String systemManagerId;

    /**
     * 系统管理员角色ID
     */
    private String systemRoleId;


}
