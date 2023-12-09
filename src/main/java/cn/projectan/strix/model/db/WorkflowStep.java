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
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-11-29
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow_step")
public class WorkflowStep extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 步骤名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 所属工作流ID
     */
    private String workflowId;

    /**
     * 步骤类型 1普通 2起点 3终点
     */
    @TableField("`type`")
    private Byte type;

    /**
     * 进入步骤时调用
     */
    private String enterInvokeId;

    /**
     * 离开步骤时调用
     */
    private String leaveInvokeId;

    public WorkflowStep(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public WorkflowStep(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
