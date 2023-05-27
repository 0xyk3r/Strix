package cn.projectan.strix.model.request.module.sms;

import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/20 19:07
 */
@Data
public class SmsConfigListReq extends BasePageReq<SmsConfig> {

    private String keyword;

}
