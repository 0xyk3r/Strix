package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueField;
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
 * Strix 系统菜单
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
@TableName("sys_system_menu")
public class SystemMenu extends BaseModel<SystemMenu> {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * 菜单权限标识
     */
    @TableField("`key`")
    private String key;

    /**
     * 菜单名称
     */
    @UniqueField(value = "菜单名称", group = 1)
    private String name;

    /**
     * 访问地址
     */
    private String url;

    /**
     * 菜单 ICON
     */
    private String icon;

    /**
     * 父菜单 ID
     */
    @UniqueField(value = "父级菜单", group = 1)
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    private Integer sortValue;

}
