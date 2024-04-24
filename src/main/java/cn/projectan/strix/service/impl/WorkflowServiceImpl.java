package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.WorkflowMapper;
import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.WorkflowService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Strix 工作流 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Override
    public CommonSelectDataResp getSelectData() {
        List<Workflow> list = getBaseMapper().selectList(Wrappers.<Workflow>lambdaQuery().select(Workflow::getId, Workflow::getName));
        return new CommonSelectDataResp(list, "id", "name", null);
    }

}
