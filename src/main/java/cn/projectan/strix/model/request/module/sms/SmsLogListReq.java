package cn.projectan.strix.model.request.module.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/22 13:24
 */
@Data
public class SmsLogListReq extends BasePageReq<SmsLog> {

    private String keyword;

    private Integer status;

    private String configKey;

}
