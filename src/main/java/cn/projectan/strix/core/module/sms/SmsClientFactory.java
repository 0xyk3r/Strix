package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.system.SmsConfig;

/**
 * SMS 客户端工厂接口
 * <p>
 * 实现此接口并注册为 Spring Bean 即可自动支持新的短信服务平台，
 * 无需修改 SmsConfigService 代码。
 *
 * @author ProjectAn
 */
public interface SmsClientFactory {

    /**
     * 该工厂支持的平台标识
     *
     * @return 平台标识，对应 {@link cn.projectan.strix.model.dict.system.SmsPlatform} 中的常量
     */
    short supportedPlatform();

    /**
     * 根据配置创建短信客户端实例
     *
     * @param config 短信配置
     * @return 短信客户端实例
     * @throws Exception 创建失败时抛出异常
     */
    StrixSmsClient createClient(SmsConfig config) throws Exception;

}
