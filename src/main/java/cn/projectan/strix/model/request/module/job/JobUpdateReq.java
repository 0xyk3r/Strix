package cn.projectan.strix.model.request.module.job;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:19
 */
@Data
public class JobUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "任务名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "任务名称长度不符合要求")
    @UpdateField
    private String name;

    @NotEmpty(groups = {InsertGroup.class}, message = "任务组不可为空")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "任务组长度不符合要求")
    private String group;

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "调用目标不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 512, message = "调用目标长度不符合要求")
    @UpdateField
    private String invokeTarget;

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "Cron 表达式不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 5, max = 128, message = "Cron 表达式长度不符合要求")
    @UpdateField
    private String cronExpression;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "计划错误策略不可为空")
    @UpdateField
    private Integer misfirePolicy;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "是否并发执行不可为空")
    @UpdateField
    private Integer concurrent;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "任务状态不可为空")
    @UpdateField
    private Integer status;

}
