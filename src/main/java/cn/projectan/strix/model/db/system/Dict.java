package cn.projectan.strix.model.db.system;

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
 * Strix 字典
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-31
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_dict")
public class Dict extends BaseModel<Dict> {

    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * 字典 Key
     */
    @TableField("`key`")
    @UniqueField("字典 Key ")
    private String key;

    /**
     * 字典名称
     */
    @TableField("`name`")
    @UniqueField("字典名称")
    private String name;

    /**
     * 字典数据类型
     *
     * @see cn.projectan.strix.model.dict.system.DictDataType
     */
    private Short dataType;

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
     * 字典版本
     */
    private Integer version;

    /**
     * 是否系统内置 0否 1是
     *
     * @see cn.projectan.strix.model.dict.common.CommonFlag
     */
    private Short provided;

}
