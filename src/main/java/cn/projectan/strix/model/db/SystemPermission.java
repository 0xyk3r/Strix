package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix 系统权限
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
@TableName("sys_system_permission")
public class SystemPermission extends BaseModel<SystemPermission> {

    @Serial
    private static final long serialVersionUID = 4L;

    /**
     * 权限名称
     */
    @UniqueDetection(value = "权限名称")
    private String name;

    /**
     * 权限标识
     */
    @UniqueDetection(value = "权限标识")
    @TableField("`key`")
    private String key;

    /**
     * 所属菜单 ID
     */
    private String menuId;

    /**
     * 权限介绍
     */
    private String description;

}
