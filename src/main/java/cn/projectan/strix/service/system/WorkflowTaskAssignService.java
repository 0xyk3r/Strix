package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.WorkflowTaskAssignMapper;
import cn.projectan.strix.model.db.system.WorkflowTaskAssign;
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

}
