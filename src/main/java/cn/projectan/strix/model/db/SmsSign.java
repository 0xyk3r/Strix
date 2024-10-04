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
 * Strix SMS 短信签名
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
@TableName("sys_sms_sign")
public class SmsSign extends BaseModel<SmsSign> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 短信配置 Key
     */
    private String configKey;

    /**
     * 签名内容
     */
    @TableField("`name`")
    private String name;

    /**
     * 签名状态
     */
    @TableField("`status`")
    private Integer status;

}
