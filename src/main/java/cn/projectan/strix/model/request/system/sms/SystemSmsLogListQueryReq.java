package cn.projectan.strix.model.request.system.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/22 13:24
 */
@Data
public class SystemSmsLogListQueryReq extends BasePageQueryReq<SmsLog> {

    private String keyword;

    private Integer status;

    private String configKey;

}
