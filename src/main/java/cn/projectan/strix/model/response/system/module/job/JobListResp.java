package cn.projectan.strix.model.response.system.module.job;

import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:08
 */
@Schema(description = "定时任务列表响应")
@Getter
public class JobListResp extends BasePageResp {

    @Schema(description = "任务列表")
    private final List<JobItem> items;

    public JobListResp(List<Job> data, Long total) {
        items = data.stream().map(d ->
                new JobItem(d.getId(), d.getName(), d.getGroup(), d.getInvokeTarget(), d.getCronExpression(), d.getMisfirePolicy(), d.getConcurrent(), d.getStatus())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "定时任务列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobItem {

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

}
