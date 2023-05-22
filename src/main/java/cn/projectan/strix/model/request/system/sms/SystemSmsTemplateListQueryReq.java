package cn.projectan.strix.model.request.system.sms;

import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/20 20:59
 */
@Data
public class SystemSmsTemplateListQueryReq extends BasePageQueryReq<SmsTemplate> {

    private String keyword;

    private Integer type;

    private Integer status;

    private String configKey;

}
