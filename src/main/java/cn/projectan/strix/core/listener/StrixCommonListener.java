package cn.projectan.strix.core.listener;

/**
 * Strix通用监听器
 * <p>
 * 用于实现 Strix 内部代码中, 向 Strix 子系统发送通知的功能
 *
 * @author ProjectAn
 * @since 2022/3/9 17:55
 */
public interface StrixCommonListener {

    /**
     * 全局通知
     *
     * @param msgType 消息类型
     * @param msgData 消息数据
     */
    void globalNotify(String msgType, Object msgData);

    /**
     * 删除系统地区通知
     *
     * @param regionId 地区ID
     */
    void deleteSystemRegionNotify(String regionId);

}
