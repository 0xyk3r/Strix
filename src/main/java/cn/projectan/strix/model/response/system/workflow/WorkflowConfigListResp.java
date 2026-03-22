package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:03
 */
@Getter
@NoArgsConstructor
@Schema(description = "工作流配置列表响应")
public class WorkflowConfigListResp extends BasePageResp {
}
