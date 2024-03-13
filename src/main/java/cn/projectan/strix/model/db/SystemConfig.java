package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * @since 2021-05-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_config")
public class SystemConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设置项标识
     */
    @TableField("`key`")
    private String key;

    /**
     * 设置项名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 设置类型 1开关 2内容
     */
    @TableField("`type`")
    private Integer type;

    /**
     * 设置项值
     */
    @TableField("`value`")
    private String value;

    /**
     * 设置说明
     */
    private String remark;


}
