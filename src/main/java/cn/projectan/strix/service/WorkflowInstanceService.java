package cn.projectan.strix.service;

import cn.projectan.strix.model.db.WorkflowInstance;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Strix 工作流实例 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
public interface WorkflowInstanceService extends IService<WorkflowInstance> {

    /**
     * 创建工作流实例
     *
     * @param workflowId 工作流ID
     * @param creatorId  创建者ID
     */
    void createInstance(String workflowId, String creatorId);

    /**
     * 后处理
     *
     * @param instance 工作流实例
     */
    void postProcess(WorkflowInstance instance);

    /**
     * 转到指定节点
     *
     * @param instance 工作流实例
     * @param nodeId   节点ID
     */
    void toNode(WorkflowInstance instance, String nodeId, boolean isBack);

    /**
     * 转到下一个节点
     *
     * @param instance 工作流实例
     */
    void toNext(WorkflowInstance instance);

}
