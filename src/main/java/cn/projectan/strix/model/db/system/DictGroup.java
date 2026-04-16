package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 字典分组
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@TableName("sys_dict_group")
public class DictGroup extends BaseModel<DictGroup> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    @UniqueField("分组名称")
    private String name;

    /**
     * 分组图标（Lucide 图标名）
     */
    private String icon;

    /**
     * 排序值
     */
    private Short sortValue;

}
