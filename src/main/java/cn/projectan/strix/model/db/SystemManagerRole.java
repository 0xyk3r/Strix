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
