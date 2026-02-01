package cn.projectan.strix.service.system;

/**
 * 聊天卡片数据提供者接口
 * <p>
 * 业务方实现此接口，提供自定义卡片类型的数据
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
public interface ChatCardDataProvider {

    /**
     * 获取支持的卡片类型
     *
     * @return 卡片类型（如 "ORDER_CARD", "PRODUCT_CARD" 等）
     */
    String getSupportedCardType();

    /**
     * 根据卡片数据 ID 获取卡片数据
     *
     * @param cardDataId 卡片数据 ID
     * @return 卡片数据（业务方自定义对象）
     */
    Object getCardData(String cardDataId);

}
