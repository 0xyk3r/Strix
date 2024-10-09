package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.service.WorkflowConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流配置缓存
 *
 * @author ProjectAn
 * @since 2024-10-07 03:16:26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowConfigCache {

    private final WorkflowConfigService workflowConfigService;

    private final Map<String, String> workflowIdMap = new HashMap<>();
    private final Map<String, String> workflowConfigIdMap = new HashMap<>();

    @PostConstruct
    private void init() {
        List<WorkflowConfig> configList = workflowConfigService.list();

        Map<String, WorkflowConfig> latestConfigs = new HashMap<>();
        for (WorkflowConfig config : configList) {
            workflowConfigIdMap.put(config.getId(), config.getContent());
            latestConfigs.merge(config.getWorkflowId(), config, (existingConfig, newConfig) ->
                    newConfig.getVersion() > existingConfig.getVersion() ? newConfig : existingConfig);
        }
        latestConfigs.forEach((workflowId, config) -> {
            workflowIdMap.put(workflowId, config.getContent());
        });
        log.info("Strix Cache: 工作流配置缓存完成, 缓存了 {} 个配置项.", workflowConfigIdMap.size());
    }

    /**
     * 更新缓存
     */
    public void reset() {
        workflowConfigIdMap.clear();
        workflowIdMap.clear();
        init();
    }

    /**
     * 获取工作流配置
     *
     * @param workflowId 工作流ID
     * @return 工作流配置
     */
    public String getConfigByWorkflowId(String workflowId) {
        return workflowIdMap.get(workflowId);
    }

    /**
     * 获取工作流配置
     *
     * @param configId 配置ID
     * @return 工作流配置
     */
    public String getConfigByConfigId(String configId) {
        return workflowConfigIdMap.get(configId);
    }

}
