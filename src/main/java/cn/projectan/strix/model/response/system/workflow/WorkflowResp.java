package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.db.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResp {

    private String id;

    private String name;

    public WorkflowResp(Workflow data) {
        this.id = data.getId();
        this.name = data.getName();
    }

}
