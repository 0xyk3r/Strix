package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PayConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
public interface PayConfigService extends IService<PayConfig> {

    /**
     * 创建支付配置
     *
     * @param payConfigList 支付配置列表
     */
    void createInstance(List<PayConfig> payConfigList);

}
