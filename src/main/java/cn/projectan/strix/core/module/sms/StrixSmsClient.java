package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/5/20 15:00
 */
@Getter
@Setter
public abstract class StrixSmsClient {

    public abstract Object get();

    public abstract void send(SmsLog sms);

    public abstract List<StrixSmsSign> getSignList();

    public abstract List<StrixSmsTemplate> getTemplateList();

    public abstract void close();

}
