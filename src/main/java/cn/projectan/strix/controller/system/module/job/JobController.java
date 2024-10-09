package cn.projectan.strix.controller.system.module.job;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
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
import cn.projectan.strix.util.UniqueChecker;
import cn.projectan.strix.util.UpdateBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统定时任务
 *
 * @author ProjectAn
 * @since 2023/7/30 16:45
 */
@Slf4j
@RestController
@RequestMapping("system/job")
@ConditionalOnProperty(prefix = "strix.module", name = "job", havingValue = "true")
@RequiredArgsConstructor
public class JobController extends BaseSystemController {

    private final JobService jobService;

    /**
     * 查询定时任务列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务列表")
    public RetResult<JobListResp> getList(JobListReq req) {
        Page<Job> page = jobService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), Job::getName, req.getKeyword())
                .page(req.getPage());
        return RetBuilder.success(new JobListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询定时任务信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务信息")
    public RetResult<JobResp> getInfo(@PathVariable String id) {
        Job job = jobService.getById(id);
        Assert.notNull(job, "定时任务不存在");

        return RetBuilder.success(
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

    /**
     * 新增定时任务
     */
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

        UniqueChecker.check(job);

        try {
            jobService.insertJob(job);
        } catch (Exception e) {
            return RetBuilder.error(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 修改定时任务
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:update')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "修改定时任务", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) JobUpdateReq req) {
        Job job = jobService.getById(id);
        Assert.notNull(job, "原记录不存在");

        UpdateBuilder.build(job, req);
        UniqueChecker.check(job);

        try {
            jobService.updateJob(job);
        } catch (Exception e) {
            return RetBuilder.error(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 删除定时任务
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:remove')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "删除定时任务", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Job job = jobService.getById(id);
        Assert.notNull(job, "原记录不存在");

        try {
            jobService.deleteJob(job);
        } catch (Exception e) {
            return RetBuilder.error(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 运行定时任务
     */
    @PostMapping("run/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:run')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "运行定时任务", operationType = SysLogOperType.OTHER)
    public RetResult<Object> run(@PathVariable String id) {
        try {
            jobService.run(id);
        } catch (Exception e) {
            return RetBuilder.error(e.getMessage());
        }

        return RetBuilder.success();
    }

}
