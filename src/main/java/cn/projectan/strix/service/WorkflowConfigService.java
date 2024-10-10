package cn.projectan.strix.service;

import cn.projectan.strix.model.db.WorkflowConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Strix 工作流配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
public interface WorkflowConfigService extends IService<WorkflowConfig> {

    /**
     * 获取最新的工作流配置
     *
     * @param workflowId 工作流ID
     * @return 最新的工作流配置
     */
    WorkflowConfig getLatestConfig(String workflowId);

}
