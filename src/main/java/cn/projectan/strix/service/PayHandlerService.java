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

    void handleSuccess(String id, String orderId);

    void handleRefund(String id, String orderId);

    void handleTimeout(String id, String orderId);

}
