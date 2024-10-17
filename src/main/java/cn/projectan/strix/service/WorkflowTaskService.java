package cn.projectan.strix.service;

import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.db.WorkflowTask;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Strix 工作流任务 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-10-09
 */
public interface WorkflowTaskService extends IService<WorkflowTask> {

    /**
     * 创建任务
     *
     * @param instance 工作流实例
     */
    void createTask(WorkflowInstance instance);

    /**
     * 完成任务
     *
     * @param taskId        任务ID
     * @param operatorId    操作人员ID
     * @param operationType 操作类型
     * @param comment       备注
     * @see cn.projectan.strix.model.dict.WorkflowOperationType 操作类型
     */
    void completeTask(String taskId, String operatorId, Byte operationType, String comment);

}
