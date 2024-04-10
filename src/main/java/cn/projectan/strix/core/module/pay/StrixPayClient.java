package cn.projectan.strix.core.module.pay;

import cn.projectan.strix.model.db.PayOrder;

import java.util.Map;

/**
 * @author ProjectAn
 * @date 2024/4/2 17:14
 */
public abstract class StrixPayClient {

    /**
     * 获取配置id
     *
     * @return 配置id
     */
    public abstract String getConfigId();

    /**
     * 获取支付配置名称
     *
     * @return 支付配置名称
     */
    public abstract String getConfigName();

    /**
     * 获取支付平台
     *
     * @return 支付平台
     */
    public abstract int getPlatform();

    public abstract Map<String, String> createWapPay(PayOrder payOrder);

    public abstract Map<String, String> createWebPay(PayOrder payOrder);

}
