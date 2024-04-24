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
import java.time.LocalDateTime;

/**
 * <p>
 * Strix 工作流
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow")
public class Workflow extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作流名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 最新配置版本编号
     */
    private Integer version;

    public Workflow(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public Workflow(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
