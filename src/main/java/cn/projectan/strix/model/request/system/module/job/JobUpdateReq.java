package cn.projectan.strix.model.request.system.module.job;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:19
 */
@Schema(description = "定时任务更新请求")
@FormSchema
@Data
public class JobUpdateReq {

    @Schema(description = "任务名称", example = "数据同步任务")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "{validation.length:field.job.name}")
    @UpdateField
    private String name;

    @Schema(description = "任务组名", example = "DEFAULT")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.job.group}")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "{validation.length:field.job.group}")
    private String group;

    @Schema(description = "调用目标字符串", example = "taskService.syncData")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.target}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 512, message = "{validation.length:field.job.target}")
    @UpdateField
    private String invokeTarget;

    @Schema(description = "Cron 执行表达式", example = "0 0/5 * * * ?")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.cron}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 128, message = "{validation.length:field.job.cron}")
    @UpdateField
    private String cronExpression;

    @Schema(description = "计划错误策略", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.misfirePolicy}")
    @UpdateField
    private Short misfirePolicy;

    @Schema(description = "是否并发执行", example = "0")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.concurrent}")
    @UpdateField
    private Short concurrent;

    @Schema(description = "任务状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.job.status}")
    @UpdateField
    private Short status;

}
