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
 * @author 安炯奕
 * @since 2021-05-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_setting")
public class SystemSetting extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设置项标识
     */
    private String settingKey;

    /**
     * 设置项名称
     */
    private String settingName;

    /**
     * 设置类型 1开关 2内容
     */
    private Integer settingType;

    /**
     * 设置项值
     */
    private String settingValue;

    /**
     * 设置说明
     */
    private String description;


}
