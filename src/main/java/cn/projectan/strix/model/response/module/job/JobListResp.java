package cn.projectan.strix.model.response.module.job;

import cn.projectan.strix.model.db.Job;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/7/30 17:08
 */
@Getter
public class JobListResp extends BasePageResp {

    private final List<JobItem> items;

    public JobListResp(List<Job> data, Long total) {
        items = data.stream().map(d -> new JobItem(d.getId(), d.getName(), d.getGroup(), d.getInvokeTarget(), d.getCronExpression(), d.getMisfirePolicy(), d.getConcurrent(), d.getStatus())).toList();
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobItem {

        private String id;

        private String name;

        private String group;

        private String invokeTarget;

        private String cronExpression;

        private Integer misfirePolicy;

        private Integer concurrent;

        private Integer status;

    }

}
