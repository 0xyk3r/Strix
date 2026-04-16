package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 * Strix 字典数据
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-28
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@TableName("sys_dict_data")
public class DictData extends BaseModel<DictData> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典 Key
     */
    @TableField("`key`")
    @UniqueField(value = "字典 Key ", group = 1)
    private String key;

    /**
     * 字典 Value
     */
    @TableField("`value`")
    @UniqueField(value = "字典 Value ", group = 1)
    private String value;

    /**
     * 字典 Label
     */
    private String label;

    /**
     * 字典排序值
     */
    private Short sort;

    /**
     * 字典样式
     */
    private String style;

    /**
     * 字典状态
     *
     * @see cn.projectan.strix.model.dict.common.CommonSwitch
     */
    private Short status;

    /**
     * 字典备注
     */
    private String remark;

    /**
     * 父级字典数据值（级联筛选用）
     */
    private String parentValue;

    /**
     * 是否默认值: 0=否, 1=是
     *
     * @see cn.projectan.strix.model.dict.common.CommonFlag
     */
    private Short isDefault;

    /**
     * 生效开始时间
     */
    private LocalDateTime validFrom;

    /**
     * 生效结束时间
     */
    private LocalDateTime validTo;

}
