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

/**
 * <p>
 *
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
public class Dict extends BaseModel {

    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * 字典 Key
     */
    @TableField("`key`")
    @UniqueDetection("字典 Key ")
    private String key;

    /**
     * 字典名称
     */
    @TableField("`name`")
    @UniqueDetection("字典名称")
    private String name;

    /**
     * 字典数据类型
     */
    private Integer dataType;

    /**
     * 字典状态
     *
     * @see cn.projectan.strix.model.dict.DictStatus
     */
    private Integer status;

    /**
     * 字典备注
     */
    private String remark;

    /**
     * 字典版本
     */
    private Integer version;

    /**
     * 数据删除状态 0正常 1删除
     *
     * @see cn.projectan.strix.model.dict.DictProvided
     */
    private Integer provided;

}
