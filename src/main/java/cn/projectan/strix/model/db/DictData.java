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
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-28
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_dict_data")
public class DictData extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典 Key
     */
    @TableField("`key`")
    @UniqueDetection(value = "字典 Key ", group = 1)
    private String key;

    /**
     * 字典 Value
     */
    @TableField("`value`")
    @UniqueDetection(value = "字典 Value ", group = 1)
    private String value;

    /**
     * 字典 Label
     */
    private String label;

    /**
     * 字典排序值
     */
    private Integer sort;

    /**
     * 字典样式
     */
    private String style;

    /**
     * 字典状态
     */
    private Integer status;

    /**
     * 字典备注
     */
    private String remark;

    public DictData(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public DictData(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
