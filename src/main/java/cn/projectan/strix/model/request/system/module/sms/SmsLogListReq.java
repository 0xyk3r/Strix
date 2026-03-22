package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/22 13:24
 */
@Data
public class SmsLogListReq extends BasePageReq<SmsLog> {

    @Size(max = 64)
    private String keyword;

    private Short status;

    private String configKey;

}
