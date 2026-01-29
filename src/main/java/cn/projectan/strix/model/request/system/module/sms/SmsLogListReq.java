package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/22 13:24
 */
@Data
public class SmsLogListReq extends BasePageReq<SmsLog> {

    private String keyword;

    private Short status;

    private String configKey;

}
