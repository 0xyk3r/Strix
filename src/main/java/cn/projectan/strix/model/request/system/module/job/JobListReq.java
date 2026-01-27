package cn.projectan.strix.model.request.system.module.job;

import cn.projectan.strix.model.db.system.Job;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/30 17:14
 */
@Data
public class JobListReq extends BasePageReq<Job> {

    private String keyword;

}
