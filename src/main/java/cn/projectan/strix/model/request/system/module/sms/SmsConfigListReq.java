package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:07
 */
@Data
public class SmsConfigListReq extends BasePageReq<SmsConfig> {

    @Size(max = 64)
    private String keyword;

}
