package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.system.StrixSmsTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-20
 */
public interface SmsTemplateService extends IService<SmsTemplate> {

        void syncTemplateList(String configId, List<StrixSmsTemplate> templateList);

}
