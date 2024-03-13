package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_payment_order")
public class PaymentOrder extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付配置ID
     */
    private String paymentConfigId;

    /**
     * 支付配置名称
     */
    private String paymentConfigName;

    /**
     * 1微信支付 2支付宝 3QQ钱包 4银联 5京东 6PayPal
     */
    private Integer paymentMethod;

    /**
     * 支付信息 各渠道格式不同 一般包含订单号流水号等内容
     */
    private String paymentData;

    /**
     * 1未支付 2已支付 3已退款
     */
    private Integer paymentStatus;

    /**
     * 支付内容提示
     */
    private String paymentTitle;

    /**
     * 支付成功后传入回调方法的信息
     */
    private String paymentAttach;

    /**
     * 支付成功时间
     */
    private LocalDateTime paymentTime;

    /**
     * 支付结果
     */
    private String paymentResponse;

    /**
     * 支付订单总金额
     */
    private Integer totalAmount;

    /**
     * 已经支付的金额
     */
    private Integer totalPayAmount;

    /**
     * 已经退款的总金额
     */
    private Integer totalRefundAmount;


}
