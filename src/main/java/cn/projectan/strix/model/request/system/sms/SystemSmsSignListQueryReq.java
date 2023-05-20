package cn.projectan.strix.model.request.system.sms;

import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/20 20:59
 */
@Data
public class SystemSmsSignListQueryReq extends BasePageQueryReq<SmsSign> {

    private String keyword;

    private Integer status;

}
