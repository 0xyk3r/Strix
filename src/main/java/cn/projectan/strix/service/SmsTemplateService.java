package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
public interface SmsTemplateService extends IService<SmsTemplate> {

    /**
     * 同步模板列表
     *
     * @param configKey    短信配置key
     * @param templateList 模板列表
     */
    void syncTemplateList(String configKey, List<StrixSmsTemplate> templateList);

}
