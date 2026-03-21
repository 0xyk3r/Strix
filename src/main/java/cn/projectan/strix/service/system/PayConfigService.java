package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.pay.PayClientFactory;
import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.mapper.system.PayConfigMapper;
import cn.projectan.strix.model.db.system.PayConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix Pay 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayConfigService extends ServiceImpl<PayConfigMapper, PayConfig> {

    private final StrixPayStore strixPayStore;
    private final List<PayClientFactory> payClientFactories;

    private Map<Short, PayClientFactory> factoryMap;

    private Map<Short, PayClientFactory> getFactoryMap() {
        if (factoryMap == null) {
            factoryMap = payClientFactories.stream()
                    .collect(Collectors.toMap(PayClientFactory::supportedPlatform, Function.identity()));
        }
        return factoryMap;
    }

    /**
     * 获取指定 configId 的支付客户端实例
     */
    public StrixPayClient getInstance(String configId) {
        return strixPayStore.getInstance(configId);
    }

    /**
     * 创建支付配置
     *
     * @param payConfigList 支付配置列表
     */
    public void createInstance(List<PayConfig> payConfigList) {
        for (PayConfig payConfig : payConfigList) {
            Assert.hasText(payConfig.getConfigData(), "Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 失败. (配置信息为空)");
            try {
                PayClientFactory factory = getFactoryMap().get(payConfig.getPlatform());
                if (factory == null) {
                    log.error("Strix Pay: 初始化支付服务实例 <{}> 失败. (不支持的支付平台: {})", payConfig.getName(), payConfig.getPlatform());
                    continue;
                }
                StrixPayClient client = factory.createClient(payConfig);
                strixPayStore.addInstance(payConfig.getId(), client);
                log.info("Strix Pay: 初始化支付服务实例 <{}> 成功.", payConfig.getName());
            } catch (Exception e) {
                log.error("Strix Pay: 初始化支付服务实例 <{}> 失败. (配置解析失败)", payConfig.getName(), e);
            }
        }
    }

}
