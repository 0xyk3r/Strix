package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix 工作流配置
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
@TableName("sys_workflow_config")
public class WorkflowConfig extends BaseModel<WorkflowConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流配置版本
     */
    private Integer version;

    /**
     * 工作流配置JSON
     */
    private String content;

}
