package cn.projectan.strix.service;

import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;

/**
 * <p>
 * Strix 工作流 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
public interface WorkflowService extends NameFetcherService<Workflow> {

    CommonSelectDataResp getSelectData();

    void saveConfig(String workflowId, String configJson);

}
