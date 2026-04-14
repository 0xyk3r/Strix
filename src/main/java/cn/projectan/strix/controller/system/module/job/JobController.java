package cn.projectan.strix.controller.system.module.job;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.model.dict.system.JobStatus;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.common.BatchModifyReq;
import cn.projectan.strix.model.request.common.BatchRemoveReq;
import cn.projectan.strix.model.request.system.module.job.JobListReq;
import cn.projectan.strix.model.request.system.module.job.JobUpdateReq;
import cn.projectan.strix.model.response.system.module.job.JobListResp;
import cn.projectan.strix.model.response.system.module.job.JobResp;
import cn.projectan.strix.service.system.JobService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@Tag(name = "系统模块 - 定时任务")
public class JobController extends BaseSystemController {

    private final JobService jobService;

    /**
     * 查询定时任务列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务列表")
    @Operation(summary = "任务列表")
    public RetResult<JobListResp> getList(JobListReq req) {
        Page<Job> page = jobService.listPage(req);
        return RetBuilder.success(new JobListResp(page.getRecords(), page.getTotal()));
    }

    /**
     * 查询定时任务信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "查询定时任务信息")
    @Operation(summary = "任务详情")
    public RetResult<JobResp> getInfo(@Parameter(description = "任务 ID") @PathVariable String id) {
        Job job = jobService.getById(id);
        Assert.notNull(job, I18nUtil.notFound("field.scheduledJob"));

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
    @StrixLog(operationGroup = "系统定时任务", operationName = "新增定时任务", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新增任务")
    public RetResult<Void> update(@RequestBody @Validated(InsertGroup.class) JobUpdateReq req) {
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
            jobService.createJob(job);
        } catch (Exception e) {
            throw new StrixException(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 修改定时任务
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:update')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "修改定时任务", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "编辑任务")
    public RetResult<Void> update(@Parameter(description = "任务 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) JobUpdateReq req) {
        Job job = jobService.getById(id);
        Assert.notNull(job, I18nUtil.notFound("field.originalData"));

        UpdateBuilder.build(job, req);
        UniqueChecker.check(job);

        try {
            jobService.updateJob(job);
        } catch (Exception e) {
            throw new StrixException(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 删除定时任务
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:remove')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "删除定时任务", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除任务")
    public RetResult<Void> remove(@Parameter(description = "任务 ID") @PathVariable String id) {
        Job job = jobService.getById(id);
        Assert.notNull(job, I18nUtil.notFound("field.originalData"));

        try {
            jobService.deleteJob(job);
        } catch (Exception e) {
            throw new StrixException(e.getMessage());
        }

        return RetBuilder.success();
    }

    /**
     * 批量删除定时任务
     */
    @PostMapping("batch/remove")
    @PreAuthorize("@ss.hasPermission('system:module:job:remove')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "批量删除定时任务", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "批量删除任务")
    public RetResult<Object> batchRemove(@RequestBody @Validated BatchRemoveReq req) {
        List<Job> jobs = jobService.listByIds(req.getIds());
        Assert.notEmpty(jobs, I18nUtil.notFound("field.scheduledJob"));

        for (Job job : jobs) {
            try {
                jobService.deleteJob(job);
            } catch (Exception e) {
                throw new StrixException(e.getMessage());
            }
        }

        return RetBuilder.success();
    }

    /**
     * 批量修改定时任务字段
     */
    @PostMapping("batch/modify")
    @PreAuthorize("@ss.hasPermission('system:module:job:update')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "批量修改定时任务字段", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "批量修改任务字段")
    public RetResult<Object> batchModify(@RequestBody @Validated BatchModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");

        switch (req.getField()) {
            case "status" -> {
                short status = Short.parseShort(req.getValue());
                Assert.isTrue(JobStatus.valid(status), "参数错误");
                List<Job> jobs = jobService.listByIds(req.getIds());
                for (Job job : jobs) {
                    job.setStatus(status);
                    try {
                        jobService.updateJob(job);
                    } catch (Exception e) {
                        throw new StrixException(e.getMessage());
                    }
                }
            }
            default -> {
                return RetBuilder.error(I18nUtil.get("error.param.invalid"));
            }
        }

        return RetBuilder.success();
    }

    /**
     * 运行定时任务
     */
    @PostMapping("run/{id}")
    @PreAuthorize("@ss.hasPermission('system:module:job:run')")
    @StrixLog(operationGroup = "系统定时任务", operationName = "运行定时任务", operationType = SystemLogOperType.OTHER)
    @Operation(summary = "立即执行任务")
    public RetResult<Void> run(@Parameter(description = "任务 ID") @PathVariable String id) {
        try {
            jobService.run(id);
        } catch (Exception e) {
            throw new StrixException(e.getMessage());
        }

        return RetBuilder.success();
    }

}
