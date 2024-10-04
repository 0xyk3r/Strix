package cn.projectan.strix.model.db;

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
 * Strix SMS 短信模板
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sms_template")
public class SmsTemplate extends BaseModel<SmsTemplate> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 短信配置 Key
     */
    private String configKey;

    /**
     * 模板CODE
     */
    @TableField("`code`")
    private String code;

    /**
     * 模板名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 模板类型
     */
    @TableField("`type`")
    private Integer type;

    /**
     * 模板状态
     */
    @TableField("`status`")
    private Integer status;

    /**
     * 模板内容
     */
    private String content;

}
