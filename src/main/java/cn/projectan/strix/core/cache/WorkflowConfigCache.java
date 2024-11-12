package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import cn.projectan.strix.service.WorkflowConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private final ObjectMapper objectMapper;

    private final List<WorkflowConfig> workflowMap = new ArrayList<>();
    private final Map<String, List<WorkflowNode>> workflowNodeMap = new HashMap<>();

    @PostConstruct
    private void init() {
        List<WorkflowConfig> configList = workflowConfigService.list();
        configList.forEach(config -> {
            workflowMap.add(config);
            try {
                List<WorkflowNode> nodes = objectMapper.readValue(config.getContent(), new TypeReference<>() {
                });
                workflowNodeMap.put(config.getId(), nodes);
            } catch (Exception e) {
                log.error("Strix Cache: 工作流配置解析失败, ID: {}", config.getId(), e);
            }
        });
        log.info("Strix Cache: 工作流配置缓存完成, 缓存了 {} 个配置项.", configList.size());
    }

    /**
     * 更新缓存
     */
    public void refresh() {
        workflowMap.clear();
        workflowNodeMap.clear();
        init();
    }

    /**
     * 获取工作流配置
     *
     * @param workflowId 工作流ID
     * @return 工作流配置
     */
    public List<WorkflowNode> getConfig(String workflowId) {
        return getConfig(workflowId, null);
    }

    public List<WorkflowNode> getConfig(String workflowId, Integer version) {
        String configId = workflowMap.stream()
                .filter(config -> config.getWorkflowId().equals(workflowId) && (version == null || config.getVersion().equals(version)))
                .max((a, b) -> b.getVersion().compareTo(a.getVersion()))
                .map(WorkflowConfig::getId)
                .orElse(null);
        return configId == null ? null : workflowNodeMap.get(configId);
    }

    public List<WorkflowNode> getConfigById(String configId) {
        return configId == null ? null : workflowNodeMap.get(configId);
    }

}
