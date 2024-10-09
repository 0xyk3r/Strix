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
 * Strix 系统角色
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
@TableName("sys_system_role")
public class SystemRole extends BaseModel<SystemRole> {

    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * 系统角色名称
     */
    @UniqueField("角色名称")
    private String name;

    /**
     * 地区权限类型
     */
    private Byte regionPermissionType;

    /**
     * 是否系统内置角色
     */
    private Byte builtin;

}
