package cn.projectan.strix.controller.system.module.job;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.module.job.JobListReq;
import cn.projectan.strix.model.request.module.job.JobUpdateReq;
import cn.projectan.strix.model.response.module.job.JobListResp;
import cn.projectan.strix.model.response.module.job.JobResp;
import cn.projectan.strix.service.JobService;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author 安炯奕
 * @date 2023/7/30 16:45
 */
@Slf4j
@RestController
@RequestMapping("system/job")
@ConditionalOnBean(Scheduler.class)
@RequiredArgsConstructor
public class JobController extends BaseSystemController {

    private final JobService jobService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务列表")
    public RetResult<JobListResp> getList(JobListReq req) {
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like(Job::getName, req.getKeyword());
        }

        Page<Job> page = jobService.page(req.getPage(), queryWrapper);
        return RetMarker.makeSuccessRsp(new JobListResp(page.getRecords(), page.getTotal()));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务信息")
    public RetResult<JobResp> getInfo(@PathVariable String id) {
        Job job = jobService.getById(id);
        Assert.notNull(job, "定时任务不存在");

        return RetMarker.makeSuccessRsp(
                new JobResp(
                        job.getId(),
                        job.getName(),
                        job.getGroup(),
                        job.getInvokeTarget(),
                        job.getCronExpression(),
                        job.getMisfirePolicy(),
                        job.getConcurrent(),
                        job.getStatus()
                )
        );
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:module:job:add')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "新增定时任务", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) JobUpdateReq req) {
        Job job = new Job(
                req.getName(),
                req.getGroup(),
                req.getInvokeTarget(),
                req.getCronExpression(),
                req.getMisfirePolicy(),
                req.getConcurrent(),
                req.getStatus()
        );
        job.setCreateBy(getLoginManagerId());
        job.setUpdateBy(getLoginManagerId());

        UniqueDetectionTool.check(job);

        try {
            jobService.insertJob(job);
        } catch (Exception e) {
            return RetMarker.makeErrRsp(e.getMessage());
        }

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:update')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "修改定时任务", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) JobUpdateReq req) {
        Job job = jobService.getById(id);
        Assert.notNull(job, "原记录不存在");

        UpdateWrapper<Job> updateWrapper = UpdateConditionBuilder.build(job, req, getLoginManagerId());
        UniqueDetectionTool.check(job);

        try {
            jobService.updateJob(job);
        } catch (Exception e) {
            return RetMarker.makeErrRsp(e.getMessage());
        }

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:remove')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "删除定时任务", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        Job job = jobService.getById(id);
        Assert.notNull(job, "原记录不存在");

        try {
            jobService.deleteJob(job);
        } catch (Exception e) {
            return RetMarker.makeErrRsp(e.getMessage());
        }

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("run/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:run')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "运行定时任务", operationType = SysLogOperType.OTHER)
    public RetResult<Object> run(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        try {
            jobService.run(id);
        } catch (Exception e) {
            return RetMarker.makeErrRsp(e.getMessage());
        }

        return RetMarker.makeSuccessRsp();
    }

}
