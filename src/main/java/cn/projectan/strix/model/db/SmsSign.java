package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-20
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sms_sign")
public class SmsSign extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 短信配置ID
     */
    private String configId;

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

    public SmsSign(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public SmsSign(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
