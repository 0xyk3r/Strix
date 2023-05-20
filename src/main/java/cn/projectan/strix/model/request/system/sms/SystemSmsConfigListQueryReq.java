package cn.projectan.strix.model.request.system.sms;

import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/20 19:07
 */
@Data
public class SystemSmsConfigListQueryReq extends BasePageQueryReq<SmsConfig> {

    private String keyword;

}
