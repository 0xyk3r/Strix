package cn.projectan.strix.model.response.system.module.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:16
 */
@Schema(description = "定时任务详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobResp {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务分组")
    private String group;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "错过执行策略")
    private Short misfirePolicy;

    @Schema(description = "是否并发执行")
    private Short concurrent;

    @Schema(description = "任务状态")
    private Short status;

}
