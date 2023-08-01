package cn.projectan.strix.model.request.module.job;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/7/30 17:19
 */
@Data
public class JobUpdateReq {

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "任务名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 64, message = "任务名称长度不符合要求")
    @UpdateField
    private String name;

    @NotEmpty(groups = {ValidationGroup.Insert.class}, message = "任务组不可为空")
    @Size(groups = {ValidationGroup.Insert.class}, min = 1, max = 64, message = "任务组长度不符合要求")
    private String group;

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "调用目标不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 5, max = 512, message = "调用目标长度不符合要求")
    @UpdateField
    private String invokeTarget;

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "Cron 表达式不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 5, max = 128, message = "Cron 表达式长度不符合要求")
    @UpdateField
    private String cronExpression;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "计划错误策略不可为空")
    @UpdateField
    private Integer misfirePolicy;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "是否并发执行不可为空")
    @UpdateField
    private Integer concurrent;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "任务状态不可为空")
    @UpdateField
    private Integer status;

}
