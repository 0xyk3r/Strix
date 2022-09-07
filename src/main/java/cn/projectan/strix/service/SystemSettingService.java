package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemSetting;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-13
 */
public interface SystemSettingService extends IService<SystemSetting> {

    /**
     * 根据key查询系统配置项
     *
     * @param key 配置项key
     * @return 系统配置项
     */
    SystemSetting selectByKey(String key);

}
