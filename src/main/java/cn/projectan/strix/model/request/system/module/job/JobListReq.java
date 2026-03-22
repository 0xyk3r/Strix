package cn.projectan.strix.model.request.system.module.job;

import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:14
 */
@Schema(description = "定时任务列表请求")
@Data
public class JobListReq extends BasePageReq<Job> {

    @Schema(description = "搜索关键词", example = "数据同步")
    @Size(max = 64)
    private String keyword;

}
