package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 20:59
 */
@Data
public class SmsTemplateListReq extends BasePageReq<SmsTemplate> {

    @Size(max = 64)
    private String keyword;

    private Short type;

    private Short status;

    private String configKey;

}
