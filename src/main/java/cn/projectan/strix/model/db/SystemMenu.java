package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("tab_system_menu")
public class SystemMenu extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单名称
     */
    @UniqueDetection("菜单名称")
    private String name;

    /**
     * 访问地址
     */
    private String url;

    /**
     * 菜单ICON
     */
    private String icon;

    /**
     * 父菜单ID
     */
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    private Integer sortValue;


}
