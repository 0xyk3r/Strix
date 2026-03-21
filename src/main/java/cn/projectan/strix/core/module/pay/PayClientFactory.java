package cn.projectan.strix.core.module.pay;

import cn.projectan.strix.model.db.system.PayConfig;

/**
 * 支付客户端工厂接口
 * <p>
 * 实现此接口并注册为 Spring Bean 即可自动支持新的支付平台，
 * 无需修改 PayConfigService 代码。
 *
 * @author ProjectAn
 */
public interface PayClientFactory {

    /**
     * 该工厂支持的平台标识
     *
     * @return 平台标识，对应 {@link cn.projectan.strix.model.dict.system.PayPlatform} 中的常量
     */
    short supportedPlatform();

    /**
     * 根据配置创建支付客户端实例
     *
     * @param config 支付配置
     * @return 支付客户端实例
     * @throws Exception 创建失败时抛出异常
     */
    StrixPayClient createClient(PayConfig config) throws Exception;

}
