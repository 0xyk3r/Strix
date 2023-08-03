package cn.projectan.strix.core.listener;

/**
 * Strix通用监听器
 * 用于实现一些需要通知到子系统的场景，比如删除了系统级地区，子系统有关联数据同时需要删除，即可通过实现本监听器来实现
 * @author 安炯奕
 * @date 2022/3/9 17:55
 */
public interface StrixCommonListener {

    void globalNotify(String msgType, Object msgData);

    void deleteSystemRegionNotify(String regionId);

}
