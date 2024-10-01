package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
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
public interface SmsSignService extends IService<SmsSign> {

    /**
     * 同步签名列表
     *
     * @param configKey 短信配置key
     * @param signList  签名列表
     */
    void syncSignList(String configKey, List<StrixSmsSign> signList);

}
