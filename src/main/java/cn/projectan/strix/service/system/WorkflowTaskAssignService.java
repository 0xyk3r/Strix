package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.WorkflowTaskAssignMapper;
import cn.projectan.strix.model.db.system.WorkflowTaskAssign;
import cn.projectan.strix.model.dict.system.WorkflowNodeType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 工作流任务分配 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-10-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskAssignService extends ServiceImpl<WorkflowTaskAssignMapper, WorkflowTaskAssign> {

    /**
     * 分页查询未处理任务指派
     *
     * @param operatorId 操作人ID
     * @param page       分页参数
     * @return 分页数据
     */
    public Page<WorkflowTaskAssign> listUnfinishedPage(String operatorId, Page<WorkflowTaskAssign> page) {
        return lambdaQuery()
                .eq(WorkflowTaskAssign::getOperatorId, operatorId)
                .isNull(WorkflowTaskAssign::getOperationType)
                .page(page);
    }

    /**
     * 分页查询已处理任务指派
     *
     * @param operatorId 操作人ID
     * @param page       分页参数
     * @return 分页数据
     */
    public Page<WorkflowTaskAssign> listFinishedPage(String operatorId, Page<WorkflowTaskAssign> page) {
        return lambdaQuery()
                .eq(WorkflowTaskAssign::getOperatorId, operatorId)
                .isNotNull(WorkflowTaskAssign::getOperationType)
                .page(page);
    }

    /**
     * 分页查询被抄送任务指派
     *
     * @param operatorId 操作人ID
     * @param page       分页参数
     * @return 分页数据
     */
    public Page<WorkflowTaskAssign> listCcPage(String operatorId, Page<WorkflowTaskAssign> page) {
        return lambdaQuery()
                .eq(WorkflowTaskAssign::getOperationType, WorkflowNodeType.CC)
                .eq(WorkflowTaskAssign::getOperatorId, operatorId)
                .page(page);
    }

}
