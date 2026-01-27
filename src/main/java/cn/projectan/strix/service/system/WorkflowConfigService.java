package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.WorkflowConfigMapper;
import cn.projectan.strix.model.db.system.WorkflowConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 工作流配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConfigService extends ServiceImpl<WorkflowConfigMapper, WorkflowConfig> {

    /**
     * 获取最新的工作流配置
     *
     * @param workflowId 工作流 ID
     * @return 最新的工作流配置
     */
    public WorkflowConfig getLatestConfig(String workflowId) {
        return lambdaQuery()
                .eq(WorkflowConfig::getWorkflowId, workflowId)
                .orderByDesc(WorkflowConfig::getVersion)
                .last("limit 1")
                .one();
    }

}
