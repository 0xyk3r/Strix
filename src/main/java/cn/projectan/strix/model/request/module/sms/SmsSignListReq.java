package cn.projectan.strix.model.request.module.sms;

import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/20 20:59
 */
@Data
public class SmsSignListReq extends BasePageReq<SmsSign> {

    private String keyword;

    private Integer status;

    private String configKey;

}
