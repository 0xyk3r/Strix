package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PayHandler;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Strix Pay 订单处理器 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-13
 */
public interface PayHandlerService extends IService<PayHandler> {

    /**
     * 处理成功
     *
     * @param id      id
     * @param orderId 订单id
     */
    void handleSuccess(String id, String orderId);

    /**
     * 处理失败
     *
     * @param id      id
     * @param orderId 订单id
     */
    void handleRefund(String id, String orderId);

    /**
     * 处理超时
     *
     * @param id      id
     * @param orderId 订单id
     */
    void handleTimeout(String id, String orderId);

}
