package cn.projectan.strix.service.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天卡片数据服务
 * <p>
 * 管理所有卡片数据提供者，提供统一的卡片数据获取接口
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Service
public class ChatCardDataService {

    /**
     * 存储 cardType -> Provider 映射
     */
    private final Map<String, ChatCardDataProvider> providerMap = new ConcurrentHashMap<>();

    /**
     * 自动发现所有 ChatCardDataProvider Bean
     */
    @Autowired(required = false)
    public void setProviders(List<ChatCardDataProvider> providers) {
        if (providers != null && !providers.isEmpty()) {
            for (ChatCardDataProvider provider : providers) {
                String cardType = provider.getSupportedCardType();
                providerMap.put(cardType, provider);
                log.info("注册卡片数据提供者: cardType={}, provider={}", cardType, provider.getClass().getSimpleName());
            }
        } else {
            log.info("未发现任何卡片数据提供者");
        }
    }

    /**
     * 获取卡片数据
     *
     * @param cardType   卡片类型
     * @param cardDataId 卡片数据 ID
     * @return 卡片数据，如果提供者不存在则返回 null
     */
    public Object getCardData(String cardType, String cardDataId) {
        ChatCardDataProvider provider = providerMap.get(cardType);
        if (provider == null) {
            log.warn("未找到卡片类型对应的提供者: cardType={}", cardType);
            return null;
        }

        try {
            return provider.getCardData(cardDataId);
        } catch (Exception e) {
            log.error("获取卡片数据失败: cardType={}, cardDataId={}", cardType, cardDataId, e);
            return null;
        }
    }

    /**
     * 检查卡片类型是否支持
     *
     * @param cardType 卡片类型
     * @return 是否支持
     */
    public boolean isSupportedCardType(String cardType) {
        return providerMap.containsKey(cardType);
    }

}
