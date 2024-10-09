package cn.projectan.strix.util;

import cn.projectan.strix.core.cache.WorkflowConfigCache;
import cn.projectan.strix.model.dict.WorkflowNodeType;
import cn.projectan.strix.model.other.module.workflow.WorkflowNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Strix 工作流 工具类
 *
 * @author ProjectAn
 * @since 2024-10-07 03:12:05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowUtil {

    private final WorkflowConfigCache workflowConfigCache;
    private final ObjectMapper objectMapper;

    public List<WorkflowNode> parseConfig(String workflowId) {
        String config = workflowConfigCache.getConfigByWorkflowId(workflowId);
        Assert.hasText(config, "工作流配置为空, workflowId: " + workflowId);
        try {
            return objectMapper.readValue(config, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Strix Cache: 工作流配置解析失败, workflowId: {}", workflowId, e);
        }
        return null;
    }

    public WorkflowNode findRootNode(List<WorkflowNode> nodes) {
        return nodes.stream()
                .filter(node -> WorkflowNodeType.ROOT.equals(node.getType()))
                .findFirst()
                .orElse(null);
    }

    public WorkflowNode findNodeById(List<WorkflowNode> nodes, String id) {
        return nodes.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public WorkflowNode findNextNode(List<WorkflowNode> nodes, String id) {
        return nodes.stream()
                .filter(node -> node.getParentId().equals(id))
                .findFirst()
                .orElse(null);
    }

}
