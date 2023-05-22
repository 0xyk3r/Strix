package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.system.StrixSmsSign;
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
public interface SmsSignService extends IService<SmsSign> {

    void syncSignList(String configKey, List<StrixSmsSign> signList);

}
