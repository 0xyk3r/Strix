package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 * Strix Pay 订单
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_pay_order")
public class PayOrder extends BaseModel<PayOrder> {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * 支付配置ID
     */
    private String configId;

    /**
     * 支付平台
     *
     * @see cn.projectan.strix.model.dict.system.PayPlatform
     */
    private Short platform;

    /**
     * 业务处理器 ID
     */
    private String handlerId;

    /**
     * 支付参数
     */
    private String params;

    /**
     * 支付状态
     *
     * @see cn.projectan.strix.model.dict.system.PayOrderStatus
     */
    @TableField("`status`")
    private Short status;

    /**
     * 支付内容标题
     */
    private String title;

    /**
     * 支付成功后回调数据
     */
    private String attach;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付成功时间
     */
    private LocalDateTime payTime;

    /**
     * 支付回调内容
     */
    private String notifyContent;

    /**
     * 支付订单总金额
     */
    private Long totalAmount;

    /**
     * 已经支付的金额
     */
    private Long totalPayAmount;

    /**
     * 已经退款的总金额
     */
    private Long totalRefundAmount;

}
