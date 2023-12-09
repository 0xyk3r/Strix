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
@TableName("sys_workflow_step_route")
public class WorkflowStepRoute extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 关系类型（成功、失败、取消、定时器、自定义）
     */
    @TableField("`type`")
    private Byte type;

    /**
     * 下一步骤ID
     */
    private String nextStepId;

    /**
     * 操作人类型 1系统管理人员 2系统用户
     */
    private Byte operatorType;

    /**
     * 操作人权限检查调用
     */
    private String operatorCheckInvokeId;

    public WorkflowStepRoute(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public WorkflowStepRoute(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
