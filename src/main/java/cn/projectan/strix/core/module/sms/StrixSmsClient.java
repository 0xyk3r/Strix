package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Strix SMS 客户端
 *
 * @author ProjectAn
 * @since 2023/5/20 15:00
 */
@Getter
@Setter
public abstract class StrixSmsClient {

    public abstract Object get();

    public abstract void send(SmsLog sms);

    public abstract List<StrixSmsSign> getSignList();

    public abstract List<StrixSmsTemplate> getTemplateList();

    @SuppressWarnings("EmptyMethod")
    public abstract void close();

}
