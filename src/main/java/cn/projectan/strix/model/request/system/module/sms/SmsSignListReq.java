package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsSign;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 20:59
 */
@Data
public class SmsSignListReq extends BasePageReq<SmsSign> {

    private String keyword;

    private Short status;

    private String configKey;

}
