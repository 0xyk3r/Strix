package cn.projectan.strix.model.request.system.module.job;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:19
 */
@Schema(description = "定时任务更新请求")
@Data
public class JobUpdateReq {

    @Schema(description = "任务名称", example = "数据同步任务")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "任务名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "任务名称长度不符合要求")
    @UpdateField
    private String name;

    @Schema(description = "任务组名", example = "DEFAULT")
    @NotEmpty(groups = {InsertGroup.class}, message = "任务组不可为空")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "任务组长度不符合要求")
    private String group;

    @Schema(description = "调用目标字符串", example = "taskService.syncData")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "调用目标不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 512, message = "调用目标长度不符合要求")
    @UpdateField
    private String invokeTarget;

    @Schema(description = "Cron 执行表达式", example = "0 0/5 * * * ?")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "Cron 表达式不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 128, message = "Cron 表达式长度不符合要求")
    @UpdateField
    private String cronExpression;

    @Schema(description = "计划错误策略", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "计划错误策略不可为空")
    @UpdateField
    private Short misfirePolicy;

    @Schema(description = "是否并发执行", example = "0")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "是否并发执行不可为空")
    @UpdateField
    private Short concurrent;

    @Schema(description = "任务状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "任务状态不可为空")
    @UpdateField
    private Short status;

}
