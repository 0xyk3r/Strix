package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author 安炯奕
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
