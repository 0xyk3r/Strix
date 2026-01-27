package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 20:59
 */
@Data
public class SmsTemplateListReq extends BasePageReq<SmsTemplate> {

    private String keyword;

    private Integer type;

    private Integer status;

    private String configKey;

}
