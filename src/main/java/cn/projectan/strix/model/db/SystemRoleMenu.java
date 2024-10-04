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
 * Strix 系统角色 菜单关系
 * </p>
 *
 * @author ProjectAn
 * @since 2021-06-21
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_role_menu")
public class SystemRoleMenu extends BaseModel<SystemRoleMenu> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 系统角色id
     */
    private String systemRoleId;

    /**
     * 系统菜单id列表
     */
    private String systemMenuId;

}
